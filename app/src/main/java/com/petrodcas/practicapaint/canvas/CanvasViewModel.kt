package com.petrodcas.practicapaint.canvas

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class CanvasViewModel() : ViewModel() {


    private var canvas: CustomCanvas? = null

    //determinan si la cola de deshacer está vacía
    private val _isEmptyUndoQueue = MutableLiveData<Boolean>(true)
    val isEmptyUndoQueue: LiveData<Boolean>
    get() = _isEmptyUndoQueue

    //determina si la cola de rehacer está vacía
    private val _isEmptyRedoQueue = MutableLiveData<Boolean>(true)
    val isEmptyRedoQueue: LiveData<Boolean>
    get() = _isEmptyRedoQueue



    /**
     * Este método establece el [CustomCanvas] como el almacenado por el viewmodel.
     *
     * Al establecerse como suyo, las propiedades [isEmptyRedoQueue] y [isEmptyUndoQueue] quedan
     * vinculadas al nuevo canvas.
     *
     * Además, en caso de que hubiese existido un canvas previo, la configuración de éste
     * será establecida en el nuevo.
     */
    fun setCustomCanvas (canvas: CustomCanvas) {
        var prevConfiguration: CustomCanvas.ConfigurationOptions? = null
        if (this.canvas != null) prevConfiguration = this.canvas!!.getConfig()
        this.canvas = canvas
        canvas.bindEmptyQueues(emptyUndoQueue = _isEmptyUndoQueue, emptyRedoQueue = _isEmptyRedoQueue)
        if (prevConfiguration != null) this.canvas!!.setConfig(prevConfiguration)
    }


    /** Deshace la última acción */
    fun undoAction () {
        this.canvas!!.undoAction()
    }

    /** Rehace la última acción deshecha */
    fun redoAction() {
        this.canvas!!.redoAction()
    }

    /** Limpia los trazos de pintura y deja el fondo con el último color seleccionado */
    fun clearDrawings() {
        this.canvas!!.clearDrawings()
    }

    /** Obtiene el color del fondo */
    fun getBgColor(): Int {
        return this.canvas!!.getConfig().bgColor!!
    }

    /** Obtiene el color del trazado */
    fun getStrokeColor(): Int {
        return this.canvas!!.getConfig().strokeColor!!
    }

    /** Establece el valor del color de fondo */
    fun setBgColor(color: Int) {
        this.canvas!!.tintBackground(color)
    }

    /** Establece el valor del color del trazado */
    fun setStrokeColor(color: Int) {
        this.canvas!!.setConfig(CustomCanvas.ConfigurationOptions(strokeColor = color))
    }

    /** Establece el modo pincel */
    fun setBrush() {
        this.canvas!!.setConfig(CustomCanvas.ConfigurationOptions(mode = CustomCanvas.Companion.Mode.BRUSH))
    }

    /** Establece el modo borrador */
    fun setEraser() {
        this.canvas!!.setConfig(CustomCanvas.ConfigurationOptions(mode = CustomCanvas.Companion.Mode.ERASER))
    }

    /** Establece el modo rectángulo */
    fun setRectangle() {
        this.canvas!!.setConfig(CustomCanvas.ConfigurationOptions(mode = CustomCanvas.Companion.Mode.RECTANGLE))
    }

    /** Establece el modo círculo */
    fun setCircle() {
        this.canvas!!.setConfig(CustomCanvas.ConfigurationOptions(mode = CustomCanvas.Companion.Mode.CIRCLE))
    }

    /** Establece el ancho del trazado */
    fun setStrokeWidth(size: Float) {
        this.canvas!!.setConfig(CustomCanvas.ConfigurationOptions(strokeWidth = size))
    }

    /** Establece si se deben rellenar las figuras o no */
    fun setFillOption(enabled: Boolean) {
        this.canvas!!.setConfig(CustomCanvas.ConfigurationOptions(isFilled = enabled))
    }

}