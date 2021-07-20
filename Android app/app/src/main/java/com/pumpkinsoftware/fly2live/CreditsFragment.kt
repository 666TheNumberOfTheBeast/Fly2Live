package com.pumpkinsoftware.fly2live

import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.pumpkinsoftware.fly2live.utils.adaptBackButton2notch


class CreditsFragment : Fragment() {
    private var account: GoogleSignInAccount? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Access control
        account = GoogleSignIn.getLastSignedInAccount(activity)
        if (account == null) {
            // Attempt to pop the controller's back stack back to a specific destination
            findNavController().popBackStack(R.id.mainFragment, false)
            return
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_credits, container, false)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Access control (continue)
        if (account == null)
            return

        var notchRects: List<Rect>? = null

        // Adapt top margin to notch
        view.setOnApplyWindowInsetsListener { _, windowInsets ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                // Get notch margin top value (0 if notch is not present)
                val notchMarginTop = windowInsets.displayCutout?.safeInsetTop ?: 0

                // Check if notch is present and device is in portrait orientation
                if (notchMarginTop > 0 && resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT) {
                    // Display the text below the notch
                    val title = view.findViewById<TextView>(R.id.menu_title)

                    // Update top margin parameter once
                    val params = title.layoutParams as ViewGroup.MarginLayoutParams
                    if (params.topMargin < notchMarginTop)
                        params.topMargin += notchMarginTop
                }

                // Get notch rects
                notchRects = windowInsets.displayCutout?.boundingRects
            }

            return@setOnApplyWindowInsetsListener windowInsets
        }

        // Set custom back button
        val btnBack = view.findViewById<ImageView>(R.id.back_button)
        btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        // Use post to wait btnBack measures
        btnBack.post {
            adaptBackButton2notch(btnBack, notchRects, activity)
        }
    }

}