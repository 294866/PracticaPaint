package com.petrodcas.practicapaint.title

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.petrodcas.practicapaint.R
import com.petrodcas.practicapaint.databinding.TitleFragmentBinding
import androidx.navigation.ui.NavigationUI

class TitleFragment : Fragment() {


    private lateinit var binding: TitleFragmentBinding
    private lateinit var viewModel: TitleViewModel


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {


        binding = DataBindingUtil.inflate(inflater,
                                            R.layout.title_fragment,
                                            container,
                                false
        )

        viewModel = ViewModelProvider(this).get(TitleViewModel::class.java)

        binding.titleViewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner


        binding.startButton.setOnClickListener {
            findNavController().navigate(R.id.action_titleFragment_to_canvasFragment)
        }

            return binding.root
    }

}