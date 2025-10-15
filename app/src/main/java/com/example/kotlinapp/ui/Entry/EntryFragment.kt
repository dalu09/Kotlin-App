package com.example.kotlinapp.ui.entry

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.kotlinapp.R

class EntryFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_entry, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Este código ha sido comentado porque EntryFragment ya no forma parte del
        // flujo de navegación. Ha sido reemplazado por la Splash Screen nativa de Android.
        // Se recomienda eliminar este archivo (EntryFragment.kt) y su layout (fragment_entry.xml).
        /*
        Handler(Looper.getMainLooper()).postDelayed({
            findNavController().navigate(R.id.action_entryFragment_to_loginFragment)
        }, 5000) // 5000 milisegundos = 5 segundos
        */
    }
}
