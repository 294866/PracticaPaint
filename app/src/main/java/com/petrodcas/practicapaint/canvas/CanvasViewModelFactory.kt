package com.petrodcas.practicapaint.canvas

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import java.lang.IllegalArgumentException

class CanvasViewModelFactory() : ViewModelProvider.Factory {


    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CanvasViewModel::class.java)) {
            return CanvasViewModel() as T
        }
        else {
            throw IllegalArgumentException("Unknown ViewModel class: " + modelClass::class.java +
                    ". Expected: " + CanvasViewModel::class.java)
        }
    }
}