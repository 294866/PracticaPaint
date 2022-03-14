package com.petrodcas.practicapaint.canvas

import android.nfc.Tag
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.petrodcas.practicapaint.R
import com.petrodcas.practicapaint.databinding.CanvasFragmentBinding


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


        return binding.root
    }


}