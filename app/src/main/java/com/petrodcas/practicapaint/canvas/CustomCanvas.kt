package com.petrodcas.practicapaint.canvas

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.databinding.ObservableField
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.petrodcas.practicapaint.R
import java.lang.Float.max
import java.lang.Float.min
import kotlin.math.abs
import kotlin.math.sqrt

class CustomCanvas @JvmOverloads constructor(
    context: Context, attributes: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attributes, defStyleAttr) {


    companion object {
        /** Modos de dibujo en los que se puede estar */
        enum class Mode {
            RECTANGLE,
            CIRCLE,
            BRUSH,
            ERASER,
            FILL_BG
        }
        //Valores por defecto de la clase
        const val DEFAULT_BACKGROUND_COLOR = R.color.white
        const val DEFAULT_STROKE_COLOR = R.color.black
        const val MIN_STROKE_WIDTH = 1
        const val MAX_STROKE_WIDTH = 255
        const val DEFAULT_STROKE_WIDTH = 30
        const val DEFAULT_FILL = false
        val DEFAULT_MODE = Mode.BRUSH
    }


    /** Data class para almacenar los datos de cada trazo del usuario */
    data class Action (
        val strokeWidth: Float? = null,
        var strokeColor: Int? = null,
        val isFilled: Boolean = false,
        val prevBGColor: Int? = null,
        val path: Path? = null,
        val mode: Mode = DEFAULT_MODE
    )

    /** Data class para almacenar la configuración del canvas */
    data class ConfigurationOptions (
        /** Lista de acciones anteriores a la actual */
        var undoQueue: ArrayDeque<Action>? = null,
        /** Lista de acciones posteriores a la actual */
        var redoQueue: ArrayDeque<Action>? = null,
        /** Anchura del trazado */
        var strokeWidth: Float? = null,
        /** Determina si se debe rellenar el interior de los elementos o no */
        var isFilled: Boolean? = null,
        /** Modo en que se encuentra el modo dibujar */
        var mode: Mode? = null,
        /** Color del fondo actual */
        var bgColor: Int? = null,
        /** Color del trazado actual */
        var strokeColor: Int? = null
    )

    /** Configuración del canvas: tiene todos los valores necesarios para llevar a cabo su función */
    private val config: ConfigurationOptions

    /** Tolerancia a la hora de realizar los movimientos en la pantalla: el movimiento realizado debe ser mayor a esta medida */
    private val touchTolerance = ViewConfiguration.get(context).scaledTouchSlop

    /** Objeto Path con el que se trabajará. Es necesario reciclarlo para mejorar el rendimiento, ya que es costoso de instanciar */
    private val touchPath: Path
    /** Objeto Paint con el que se trabajará. Es necesario reciclarlo para mejorar el rendimiento, ya que es costoso de instanciar */
    private val usedPaint: Paint

    /** Posición actual del toque del usuario en el eje X*/
    private var currentX: Float
    /** Posición actual del toque del usuario en el eje Y */
    private var currentY: Float

    /** Flag para identificar si se dibujó algo y discriminar a los path vacíos de ser incluidos en el listado de dibujo */
    private var drawingDone: Boolean






    /** Determina si la cola de deshacer está vacía. Esta propiedad debe ser importada desde el viewmodel. */
    private lateinit var isEmptyUndoQueue : MutableLiveData<Boolean>
    /** Determina si la cola de rehacer está vacía. Esta propiedad debe ser importada desde el viewmodel. */
    private lateinit var isEmptyRedoQueue: MutableLiveData<Boolean>


    /**
     * Importa los LiveData del viewmodel para poder actualizar los datos en tiempo real del estado de las colas
     * sin obtener referencia directa al viewmodel
     */
    fun bindEmptyQueues (emptyUndoQueue: MutableLiveData<Boolean>, emptyRedoQueue: MutableLiveData<Boolean>) {
        isEmptyRedoQueue = emptyRedoQueue
        isEmptyUndoQueue = emptyUndoQueue
        isEmptyUndoQueue.value = config.undoQueue!!.isEmpty()
        isEmptyRedoQueue.value = config.redoQueue!!.isEmpty()
    }


    //constructor
    init {
        Log.d(":::INIT","Se ha creado un nuevo objeto...")

        //se inicializan los valores por defecto
        config = ConfigurationOptions(
            undoQueue = ArrayDeque(),
            redoQueue = ArrayDeque(),
            strokeWidth = DEFAULT_STROKE_WIDTH.toFloat(),
            isFilled = DEFAULT_FILL,
            mode = DEFAULT_MODE,
            bgColor = context.getColor(DEFAULT_BACKGROUND_COLOR),
            strokeColor = context.getColor(DEFAULT_STROKE_COLOR)
        )

        //se inicializa la posición de las coordenadas de toque
        currentX = 0f
        currentY = 0f

        //se inicializa un nuevo paint que será reutilizado
        usedPaint = Paint().apply {
            color = config.strokeColor!!
            // Smooths out edges of what is drawn without affecting shape.
            isAntiAlias = true
            // Dithering affects how colors with higher-precision than the device are down-sampled.
            isDither = true
            style = determineStrokeStyle() // default: FILL
            strokeJoin = Paint.Join.ROUND // default: MITER
            strokeCap = Paint.Cap.ROUND // default: BUTT
            strokeWidth = config.strokeWidth!!// default: Hairline-width (really thin)
        }

        //se inicializa un nuevo Path que será reutilizado
        touchPath = Path()
        drawingDone = false
    }




    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        //se pinta el fondo del bitmap al del color de fondo actual
        canvas.drawColor(config.bgColor!!)
        //se dibujan los elementos pintados hasta el momento
        drawPath(canvas)
    }


    /**
     * Dibuja las acciones guardadas en la undoQueue de [config] según sea necesario.
     * - Si el modo es [Mode.ERASER], entonces es necesario cambiar el color al del fondo actual (el valor podría diferir desde entonces)
     * - Si el modo es [Mode.FILL_BG] es necesario saltarlo, ya que esa acción no se debe pintar según el diseño de esta aplicación
     * - Para el resto de modos, se dibuja el Path sin más.
     */
    private fun drawPath (canvas: Canvas) {

        //Dibuja cada elemento existente en undoQueue según los datos guardados.
        for (a:Action in config.undoQueue!!) {
            when (a.mode) {
                //si era modo cambiar de color el fondo, no hace falta hacer nada aquí (actualmente ya existe dibujado el color de fondo más reciente)
                Mode.FILL_BG -> continue
                else -> {
                    updatePaint(a)
                    canvas.drawPath(a.path!!, usedPaint)
                }
            }
        }
        //se dibuja el path actual, pues no se añade a la cola hasta haberse terminado el evento de toque
        updatePaint()
        canvas.drawPath(touchPath, usedPaint)

    }


    override fun onTouchEvent(event: MotionEvent): Boolean {
        //actua acorde al tipo de evento recibido
        when(event.action) {
            MotionEvent.ACTION_DOWN -> onTouchStart(event.x, event.y)
            MotionEvent.ACTION_MOVE -> onTouchMove(event.x, event.y)
            MotionEvent.ACTION_UP -> onTouchEnd(event.x, event.y)
        }
        //solo actualiza el canvas si se ha dibujado algo. Es una medida de optimización.
        if (drawingDone) invalidate()
        return true
    }


    /**
     * Gestiona qué ocurre cuando el usuario toca la pantalla
     */
    private fun onTouchStart(newX: Float, newY: Float) {
        //se mueve el comienzo del path a donde se tocó y se actualizan los coordenadas actuales
        touchPath.moveTo(newX, newY)
        currentX = newX
        currentY = newY
    }


    /**
     * Gestiona qué ocurre cuando el usuario mueve el dedo (o lo que sea con que esté tocando la pantalla).
     *
     * @param newX Nuevo valor de la coordenada X. Es decir, donde el usuario ha parado el dedo.
     * @param newY Nuevo valor de la coordenada Y. Es decir, donde el usuario ha parado el dedo.
     */
    private fun onTouchMove(newX: Float, newY: Float) {
        //se calculan las diferencias entre los valores nuevos y viejos de las coordenadas
        // para saber la distancia que se he desplazado en la pantalla
        val deltaX = Math.abs(newX - currentX)
        val deltaY = Math.abs(newY - currentY)

        //si se supera la tolerancia de toque, entonces se puede dibujar, si no, no
        if (deltaX >= touchTolerance || deltaY >= touchTolerance) {
            Log.d(":::onTouchMove", "Se ha superado la tolerancia")
            when (config.mode) {
                Mode.BRUSH, Mode.ERASER -> drawLine(newX, newY)
                Mode.RECTANGLE -> drawRectangle(newX, newY)
                Mode.CIRCLE -> drawOval(newX, newY)
                else -> return //no se hace nada
            }
        }
    }



    /** Dibuja una línea recta */
    private fun drawLine (newX: Float, newY: Float) {
        touchPath.quadTo(
            currentX,
            currentY,
            (currentX + newX)/2f,
            (currentY + newY)/2f
        )
        currentX = newX
        currentY = newY
        drawingDone = true
    }


    /**
     * Dibuja un rectángulo desde su lado izquierdo superior
     */
    private fun drawRectangle (newX: Float, newY: Float) {
        //es necesario reiniciar el path para no crear múltiples rectángulos
        touchPath.reset()

        val left = min(currentX, newX)
        val right = max(currentX, newX)
        val bottom = max(currentY, newY)
        val top = min(currentY, newY)

        touchPath.addRect(left, top, right, bottom, Path.Direction.CW)

        drawingDone = true
    }


    /**
     * Dibuja un círculo perfecto centrado en el punto clickado
     */
    private fun drawCircle (newX: Float, newY: Float) {
        //se resetea el path para que no se dibujen múltiples círculos
        touchPath.reset()

        val dx = abs(newX - currentX)
        val dy= abs(newY - currentY)
        val radius = sqrt((dx*dx) + (dy*dy) )

        touchPath.addCircle(currentX, currentY, radius, Path.Direction.CW)

        drawingDone = true
    }


    /**
     * Dibuja un óvalo desde su lado izquierdo
     */
    private fun drawOval (newX: Float, newY: Float) {
        //se resetea el path para que no se dibujen múltiples óvalos
        touchPath.reset()

        val left = min(currentX, newX)
        val top = min (currentY, newY)
        val right = max (currentX, newX)
        val bottom = max (currentY, newY)

        touchPath.addArc(left, top, right, bottom, 0f, 360f)

        drawingDone = true
    }

    /**
     * Gestiona lo que ocurre cuando el usuario levanta el dedo de la pantalla tras un evento de toque
     */
    private fun onTouchEnd(newX: Float, newY: Float) {
        //si se dibujó algo, entonces añade un nuevo Action a la cola de cosas hechas
        if (drawingDone) {
            addToUndoQueue(generateActionFromConfig())
        }
        //limpia la cola de rehacer
        config.redoQueue!!.clear()
        setIsEmptyRedoQueueValue(true)
        //restablece los valores de las coordenadas x e y actuales
        currentY = 0f
        currentX = 0f
        //restablece el valor de si se ha dibujado
        drawingDone = false
        //reinicia el path para reutilizarlo: mayor eficiencia
        touchPath.reset()
    }




    /**
     * Establece los valores de la configuración del canvas en base a los valores recibidos.
     *
     * Los valores nulos son ignorados, por lo que puede usarse este método para establecer casi
     * cualquier atributo.
     *
     * No obstante, no se debe utilizar esta opción para cambiar el valor de [ConfigurationOptions.bgColor] a
     * menos que sea para inicializar los valores a los de un canvas anterior.
     *
     * Si se desea cambiar el color del fondo, se debe usar el método [tintBackground].
     */
    fun setConfig (options: ConfigurationOptions) {
        if (options.undoQueue != null) config.undoQueue = options.undoQueue
        if (options.redoQueue != null) config.redoQueue = options.redoQueue
        if (options.strokeWidth!= null) config.strokeWidth = options.strokeWidth
        if (options.isFilled != null) config.isFilled = options.isFilled
        if (options.mode != null) config.mode = options.mode
        if (options.bgColor != null) config.bgColor = options.bgColor
        if (options.strokeColor != null) config.strokeColor = options.strokeColor

        if (options.undoQueue != null || options.redoQueue != null) {
            //se fuerza a redibujar la escena, ya que se han modificado los historiales
            invalidate()
        }
        else {
            //no es necesario redibujar, pero se necesita actualizar el usedPaint
            updatePaint()
        }
    }

    /**
     * Devuelve la configuración actual.
     *
     * @return [config]
     */
    fun getConfig () : ConfigurationOptions {
        return this.config
    }



    /**
     * Modifica el color del fondo y agrega un Action especial a la cola de deshacer.
     */
    fun tintBackground (newColor: Int) {
        //se almacenan los valores actuales
        var prevColor = config.bgColor
        var prevMode = config.mode
        //se cambian los del archivo de configuración a los nuevos
        config.mode = Mode.FILL_BG
        config.bgColor = newColor
        //se agrega una acción generada a la cola de deshacer
        addToUndoQueue(generateActionFromConfig(prevColor))
        //se devuelve el estado del modo de la configuración al previo
        config.mode = prevMode
        invalidate()
    }


    /**
     * Añade la acción a la cola de deshacer
     */
    private fun addToUndoQueue(a: Action) {
        config.undoQueue!!.addLast(a)
        setIsEmptyUndoQueueValue(false)
    }

    /**
     * Añade la acción a la cola de rehacer
     */
    private fun addToRedoQueue(a: Action) {
        config.redoQueue!!.addLast(a)
        setIsEmptyRedoQueueValue(false)
    }

    private fun setIsEmptyUndoQueueValue (b: Boolean) {
        if (isEmptyUndoQueue != null) isEmptyUndoQueue.value = b
    }

    private fun setIsEmptyRedoQueueValue (b: Boolean) {
        if (isEmptyRedoQueue != null) isEmptyRedoQueue.value = b
    }

    /**
     * Deshace una acción.
     *
     * La acción deshecha es añadida a la cola de rehacer.
     */
    fun undoAction () {
        if (config.undoQueue!!.isNotEmpty()) {
            val action = config.undoQueue!!.removeLast()
            addToRedoQueue(action)
            setIsEmptyUndoQueueValue(config.undoQueue!!.isEmpty())
            //si la acción extraida era pintar el fondo, lo gestiona
            if (action.mode == Mode.FILL_BG) {
                config.bgColor = action.prevBGColor
            }

            invalidate()
        }
    }


    /**
     * Rehace una acción.
     *
     * La acción rehecha es añadida a la cola de deshacer.
     */
    fun redoAction() {
        if (config.redoQueue!!.isNotEmpty()) {
            val action = config.redoQueue!!.removeLast()
            addToUndoQueue(action)
            setIsEmptyRedoQueueValue(config.redoQueue!!.isEmpty())
            //si la acción a realizar era pintar el fondo, la gestiona
            if (action.mode == Mode.FILL_BG) {
                config.bgColor = action.strokeColor
            }

            invalidate()
        }
    }


    /**
     * Deshace todo el historial de dibujo, pero deja el fondo del último color seleccionado.
     *
     */
    fun clearDrawings() {
        config.undoQueue!!.clear()
        config.redoQueue!!.clear()
        setIsEmptyUndoQueueValue(true)
        setIsEmptyRedoQueueValue(true)
        invalidate()
    }



    /**
     * Actualiza los valores de [usedPaint] de acuerdo a los valores de [a] y
     * el [Mode] que almacena.
     * @param a El [Action] con los valores a actualizar.
     */
    private fun updatePaint (a: Action) {

        when (a.mode) {
            Mode.BRUSH -> updatePaintValues(a.strokeColor, a.strokeWidth)
            Mode.CIRCLE, Mode.RECTANGLE -> updatePaintValues(a.strokeColor, a.strokeWidth, a.isFilled!!)
            Mode.ERASER -> updatePaintValues(config.bgColor, a.strokeWidth)

           else -> return
        }
    }


    /**
     * Método auxiliar para acortar las otras implementaciones de actualización de [usedPaint].
     *
     * @param color El valor para [Paint.setColor]. Si se establece null, se usará el color ya existente.
     * @param strokeWidth El valor para [Paint.setStrokeWidth]. Si se establece a null, se usará el valor ya existente.
     * @param isFilled El valor para determinar [Paint.Style] según el método [determineStrokeStyle].
     */
    private fun updatePaintValues (color: Int? = null, strokeWidth: Float? = null, isFilled: Boolean = false) {
        if (color != null) usedPaint.color = color
        if (strokeWidth != null) usedPaint.strokeWidth = strokeWidth
        usedPaint.style = determineStrokeStyle(isFilled)
    }


    /**
     * Actualiza los valores de [usedPaint] de acuerdo a los valores guardados en [config]
     * y el modo en que se encuentra el canvas.
     *
     * @param c Un archivo de configuración [ConfigurationOptions] opcional. Por defecto se utiliza [config].
     */
    private fun updatePaint (c: ConfigurationOptions = config) {
        when (c.mode) {
            Mode.BRUSH -> updatePaintValues(c.strokeColor!!, c.strokeWidth!!)
            Mode.RECTANGLE, Mode.CIRCLE -> {
                updatePaintValues(c.strokeColor!!, c.strokeWidth!!, c.isFilled!!)
            }
            Mode.ERASER -> {
                updatePaintValues(c.bgColor!!, c.strokeWidth!!)
            }
            else -> return //no es necesario hacer nada en el resto de casos
        }
    }



    /**
     * Genera un [Action] en base a los parámetros guardados en [config] y el modo en que se encuentre
     * actualmente el canvas.
     *
     * No todos los modos necesitan la misma configuración, por lo que se evita guardar o crear más de
     * un objeto Paint al no tener que sobrescribir propiedades innecesarias.
     *
     * @param oldBgColor El antiguo color fondo utilizado por la aplicación en caso de que en la configuración
     * se halle el modo [Mode.FILL_BG].
     *
     * Para el resto de [Mode] este valor es nulo.
     *
     * @return Un objecto [Action] acorde a los datos del archivo de configuración y el [Mode] en que se encuentra
     * el canvas.
     */
    private fun generateActionFromConfig (oldBgColor: Int? = null) : Action {
        return when (config.mode!!) {

            Mode.BRUSH -> {
                Action(
                    strokeColor = config.strokeColor,
                    strokeWidth = config.strokeWidth,
                    mode = config.mode!!,
                    path = Path(touchPath)
                )
            }

            Mode.ERASER -> {
                Action(
                    strokeColor = config.bgColor,
                    strokeWidth = config.strokeWidth,
                    mode = config.mode!!,
                    path = Path(touchPath)
                )
            }

            Mode.CIRCLE, Mode.RECTANGLE -> {
                Action(
                    strokeColor = config.strokeColor,
                    strokeWidth = config.strokeWidth,
                    mode = config.mode!!,
                    path = Path(touchPath),
                    isFilled = config.isFilled!!
                )
            }

            Mode.FILL_BG -> {
                Action(
                    strokeColor = config.bgColor,
                    prevBGColor = oldBgColor,
                    mode = config.mode!!
                )
            }
        }
    }


    /**
     * Devuelve el estilo de trazado necesario según el valor de [isFilled].
     *
     * @param isFilled El valor para el que se desea comprobar. Por defecto se utiliza el
     * valor del isFilled almacenado en [config].
     *
     * @return [Paint.Style.FILL_AND_STROKE] si [isFilled] es **true** o [Paint.Style.STROKE] en caso contrario.
     */
    private fun determineStrokeStyle(isFilled: Boolean = config.isFilled!!) : Paint.Style {
        return if (isFilled) Paint.Style.FILL_AND_STROKE else Paint.Style.STROKE
    }


}