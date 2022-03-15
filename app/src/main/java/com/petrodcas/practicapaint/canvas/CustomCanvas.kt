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
import com.petrodcas.practicapaint.R

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
        const val MIN_STROKE_WIDTH = 1f
        const val MAX_STROKE_WIDTH = 255f
        const val DEFAULT_STROKE_WIDTH = 60f
        const val DEFAULT_FILL = false
        val DEFAULT_MODE = Mode.BRUSH
    }


    /** Data class para almacenar los datos de cada trazo del usuario */
    data class Action (
        val strokeWidth: Float? = null,
        var strokeColor: Int? = null,
        val isFilled: Boolean? = null,
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

    //constructor
    init {
        Log.d(":::INIT","Se ha creado un nuevo objeto...")
        //se inicializan los valores por defecto
        config = ConfigurationOptions(
            undoQueue = ArrayDeque(),
            redoQueue = ArrayDeque(),
            strokeWidth = DEFAULT_STROKE_WIDTH,
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
            style = Paint.Style.STROKE // default: FILL
            strokeJoin = Paint.Join.ROUND // default: MITER
            strokeCap = Paint.Cap.ROUND // default: BUTT
            strokeWidth = config.strokeWidth!!// default: Hairline-width (really thin)
        }
        //se inicializa un nuevo Path que será reutilizado
        touchPath = Path()
        drawingDone = false
    }




    override fun onDraw(canvas: Canvas) {
        Log.d(":::onDraw", "se ha entrado al método")
        super.onDraw(canvas)
        //se pinta el fondo del bitmap al del color de fondo actual
        canvas.drawColor(config.bgColor!!)
        Log.d(":::onDraw", "Se intentan dibujar los elementos")
        //se dibujan los elementos pintados hasta el momento
        drawPath(canvas)
        Log.d(":::onDraw", "saliendo del método")
    }


    /**
     * Dibuja las acciones guardadas en la undoQueue de [config] según sea necesario.
     * - Si el modo es [Mode.ERASER], entonces es necesario cambiar el color al del fondo actual (el valor podría diferir desde entonces)
     * - Si el modo es [Mode.FILL_BG] es necesario saltarlo, ya que esa acción no se debe pintar según el diseño de esta aplicación
     * - Para el resto de modos, se dibuja el Path sin más.
     */
    private fun drawPath (canvas: Canvas) {

        //por cada trío de datos en la data class Action almacenada en la cola de "deshacer" (acciones realizadas hasta el momento)
        for (a: Action in config.undoQueue!!) {
            //Log.d(":::drawPath", "Se procede a filtrar por modo.")
            //realiza acciones según el modo de la acción
            when (a.mode) {
                //si es modo el borrador, entonces cambia el color de su pintura al del color de fondo actual y después dibuja
                Mode.ERASER -> {
                    a.strokeColor = config.bgColor!!
                    updatePaint(a)
                    canvas.drawPath(a.path!!, usedPaint)
                }//si era modo cambiar de color el fondo, no hace falta hacer nada aquí (actualmente ya existe dibujado el color de fondo más reciente)
                Mode.FILL_BG -> continue
                else -> {//para cualquier otro caso, dibuja lo que corresponda tal y como se hizo en su momento
                    //Log.d(":::drawPath", "Se determina que se trata de un else. Se procede a dibujarlo en el canvas")
                    updatePaint(a)
                    canvas.drawPath(a.path!!, usedPaint!!)
                }

            }//fin del when

        }//fin del for

        //dibuja el path actual
        updatePaint()
        canvas.drawPath(touchPath, usedPaint)

    }//fin de la función





    override fun onTouchEvent(event: MotionEvent): Boolean {
        when(event.action) {
            MotionEvent.ACTION_DOWN -> onTouchStart(event.x, event.y)
            MotionEvent.ACTION_MOVE -> onTouchMove(event.x, event.y)
            MotionEvent.ACTION_UP -> onTouchEnd(event.x, event.y)
        }
        if (drawingDone) invalidate()
        return true
    }


    private fun onTouchStart(newX: Float, newY: Float) {
        //Log.d(":::onTouchStart", "Se accede al método")
        touchPath.moveTo(newX, newY)
        currentX = newX
        currentY = newY
        //Log.d(":::onTouchStart", "Se sale del método")
    }

    private fun onTouchMove(newX: Float, newY: Float) {
        //Log.d(":::onTouchMove", "Se accede al método")
        val deltaX = Math.abs(newX - currentX)
        val deltaY = Math.abs(newY - currentY)

        if (deltaX >= touchTolerance || deltaY >= touchTolerance) {
            //Log.d(":::onTouchMove", "Se ha superado la tolerancia")
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
        //Log.d(":::onTouchMove", "Se sale del método")
    }

    private fun onTouchEnd(newX: Float, newY: Float) {
        //Log.d(":::onTouchEnd", "Se accede al método")
        if (drawingDone) {
            config.undoQueue!!.addLast(Action(strokeWidth = config.strokeWidth, strokeColor =  config.strokeColor, isFilled = config.isFilled, path = Path(touchPath), mode = config.mode!!))
        }
        config.redoQueue!!.clear()
        currentY = 0f
        currentX = 0f
        drawingDone = false
        touchPath.reset()
        //Log.d(":::onTouchEnd", "Se sale del método")
    }



    /**
     * Establece los valores de la configuración del canvas en base a los valores recibidos
     */
    fun setConfig (options: ConfigurationOptions) {
        if (options.undoQueue != null) config.undoQueue = options.undoQueue
        if (options.redoQueue != null) config.redoQueue = options.redoQueue
        if (options.strokeWidth!= null) config.strokeWidth = options.strokeWidth
        if (options.isFilled != null) config.isFilled = options.isFilled
        if (options.mode != null) config.mode = options.mode
        if (options.bgColor != null) config.bgColor = options.bgColor
        if (options.strokeColor != null) config.strokeColor = options.strokeColor
    }




    private fun updatePaint (a: Action) {

        when (a.mode) {
            Mode.BRUSH -> {
                usedPaint.color = a.strokeColor!!
                usedPaint.strokeWidth = a.strokeWidth!!
            }
        }
    }


    private fun updatePaint (c: ConfigurationOptions = config) {
        when (c.mode) {
            Mode.BRUSH -> {
                usedPaint.color = c.strokeColor!!
                usedPaint.strokeWidth = c.strokeWidth!!
            }
        }
    }




}