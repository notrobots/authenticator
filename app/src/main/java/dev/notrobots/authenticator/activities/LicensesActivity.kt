package dev.notrobots.authenticator.activities

import android.os.Bundle
import dev.notrobots.authenticator.R
import dev.notrobots.authenticator.extensions.resolveColorAttribute
import dev.notrobots.authenticator.util.parseHEXColor

class LicensesActivity : ThemedActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val colors = mapOf(
            "backgroundColor" to resolveColorAttribute(R.attr.backgroundColor),
            "colorOnBackground" to resolveColorAttribute(R.attr.colorOnBackground),
            "colorPrimary" to resolveColorAttribute(R.attr.colorPrimary),
            "colorPrimaryVariant" to resolveColorAttribute(R.attr.colorPrimaryVariant)
        )
        val css = "body{background-color:{backgroundColor};color:{colorOnBackground};font-family:sans-serif;padding-bottom:30px;}h2{color:{colorPrimary};font-weight:600;margin-top:40px;}pre{white-space:pre-line;word-wrap:break-word;}a:link{color:{colorPrimary};}a:visited{color:{colorPrimaryVariant};}a:hover{color:{colorPrimaryVariant};}a:active{color:{colorPrimary};}"
        var html = buildString {
            append("<style>$css</style>")
            append("<body>")
            append("</body>")
        }

        for ((color, value) in colors) {
            html = html.replace("{$color}", parseHEXColor(value))
        }

        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_add)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

//        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)

//        setSupportActionBar(toolbar)
//        supportActionBar?.setTitle(R.string.pref_licenses)
//        toolbar.setNavigationOnClickListener {
//            finish()
//        }
    }
}