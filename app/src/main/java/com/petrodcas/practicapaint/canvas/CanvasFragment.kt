package com.petrodcas.practicapaint.canvas

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.petrodcas.practicapaint.R
import com.petrodcas.practicapaint.databinding.CanvasFragmentBinding
import vadiole.colorpicker.ColorModel
import vadiole.colorpicker.ColorPickerDialog


class CanvasFragment : Fragment() {

    private lateinit var binding: CanvasFragmentBinding
    private lateinit var viewModel: CanvasViewModel


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {


        binding = DataBindingUtil.inflate(inflater,
            R.layout.canvas_fragment,
            container,
            false
        )


        viewModel = ViewModelProvider(this, CanvasViewModelFactory(requireNotNull(context)))[CanvasViewModel::class.java]


        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        //se asigna el nuevo canvas al modelview
        viewModel.setCustomCanvas(binding.customCanvas)

        //se asignan listeners a los botones de hacer/deshacer
        binding.undoOptionButton.setOnClickListener{ viewModel.undoAction() }
        binding.redoOptionButton.setOnClickListener{ viewModel.redoAction() }

        //se asigna listener al botón de pintar el fondo
        binding.tintBackgroundOption.setOnClickListener {
            showColorPicker(viewModel.getBgColor()) { c -> viewModel.setBgColor(c) }
        }

        //se asigna listener al botón de seleccionar color
        binding.colorpickerOptionButton.setOnClickListener {
            showColorPicker(viewModel.getStrokeColor()) {c -> viewModel.setStrokeColor(c)}
        }

        //se asigna un listener a la barra deslizante de selección de tamaño
        binding.penSizeSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                viewModel.setStrokeWidth(p1.toFloat())
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {}

            override fun onStopTrackingTouch(p0: SeekBar?) {}
        })

        //se asigna un listener al switch de elección de relleno
        binding.fillButton.setOnCheckedChangeListener { _, b -> viewModel.setFillOption(b) }

        return binding.root
    }




    /** Muestra un colorPicker acorde a los parámetros introducidos.
     *
     * @param initialColor color con el que se comienza la selección
     * @param colorFunction función a realizar con el color seleccionado por el usuario
     *
     */
    private fun showColorPicker (initialColor: Int, colorFunction: (Int) -> Unit) {
        val picker: ColorPickerDialog = ColorPickerDialog.Builder()
            //  set initial (default) color
            .setInitialColor(initialColor)
            //  set Color Model. ARGB, RGB or HSV
            .setColorModel(ColorModel.RGB)
            //  set is user be able to switch color model
            .setColorModelSwitchEnabled(false)
            //  set your localized string resource for OK button
            .setButtonOkText(android.R.string.ok)
            //  set your localized string resource for Cancel button
            .setButtonCancelText(android.R.string.cancel)
            //  callback for picked color (required)
            .onColorSelected(colorFunction)
            //  create dialog
            .create()
        //  show dialog from Fragment
        picker.show(childFragmentManager, "color_picker")
    }


}