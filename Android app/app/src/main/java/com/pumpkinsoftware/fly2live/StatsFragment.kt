package com.pumpkinsoftware.fly2live

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import androidx.activity.addCallback
import androidx.navigation.fragment.findNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount


class StatsFragment : Fragment() {
    private var account: GoogleSignInAccount? = null

    // The pager widget, which handles animation and
    // allows swiping horizontally to access previous and next wizard steps
    private lateinit var mPager: ViewPager2

    companion object {
        // The number of pages (wizard steps) to show
        const val NUM_PAGES = 3
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Access control
        account = GoogleSignIn.getLastSignedInAccount(activity)
        if (account == null) {
            // Attempt to pop the controller's back stack back to a specific destination
            findNavController().popBackStack(R.id.mainFragment, false)
            return
        }

        activity?.onBackPressedDispatcher?.addCallback {
            if (mPager.currentItem == 0) {
                // If the user is currently looking at the first step,
                // allow the system to handle the Back button
                isEnabled = false
                activity?.onBackPressed()
            } else {
                // Otherwise, select the first step
                mPager.currentItem = 0
                // Otherwise, select the previous step
                //mPager.currentItem = mPager.currentItem - 1
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_stats, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Access control (continue)
        if (account == null)
            return

        mPager = view.findViewById(R.id.pager)

        // The pager adapter, which provides the pages to the view pager widget
        mPager.adapter = ScreenSlidePagerAdapter(this)
    }

    private inner class ScreenSlidePagerAdapter(fm: Fragment) : FragmentStateAdapter(fm) {
        override fun createFragment(position: Int): Fragment {
            // Since all Fragment classes you create must have a public, no-arg constructor,
            // use the arguments Bundle to give additional info to the fragment
            val args = Bundle()
            args.putInt("position", position)

            val f = StatsSlideFragment()
            f.arguments = args

            return f
        }

        override fun getItemCount(): Int {
            return NUM_PAGES
        }
    }
}