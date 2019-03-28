package vn.zenity.football

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import vn.zenity.football.R
import vn.zenity.football.extensions.HelpPager
import kotlinx.android.synthetic.main.activity_tutorial.*

class TutorialActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tutorial)
        val padding =  (resources.displayMetrics.density * 30f).toInt()
        viewPager.pageMargin = padding
        viewPager.clipToPadding = false
        viewPager.setPadding(padding, padding, padding, 0)
        viewPager.adapter = HelpPager()
        back.setOnClickListener {
            finish()
        }
        skip.setOnClickListener {
            finish()
        }
        indicator.setViewPager(viewPager)
    }
}
