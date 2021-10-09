package dev.notrobots.authenticator.views

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.media.Image
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.widget.ImageViewCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import dev.notrobots.androidstuff.extensions.resolveColorAttribute
import dev.notrobots.androidstuff.util.logd
import dev.notrobots.authenticator.R
import kotlinx.android.synthetic.main.view_imageslider.view.*

class ImageSlider(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int
) : FrameLayout(context, attrs, defStyleAttr) {
    private val activeColor: Int
    private val disabledColor: Int
    private val images = mutableListOf<Drawable>()
    private val imageAdapter = ImageAdapter()
    var currentImage: Drawable? = null
        private set
    var currentImageIndex: Int = -1
        private set

    init {
        inflate(context, R.layout.view_imageslider, this)

        activeColor = context.resolveColorAttribute(R.attr.colorPrimary)
        disabledColor = Color.LTGRAY

        imageslider_pager.adapter = imageAdapter
        imageslider_pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)

                currentImageIndex = position
                currentImage = images[currentImageIndex]
                updateSliderControls()
            }
        })
        imageslider_indicator.text = null
        imageslider_previous.setOnClickListener {
            previous()
        }
        imageslider_next.setOnClickListener {
            next()
        }
    }

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context) : this(context, null, 0)

    fun setImageResources(images: List<Int>) {
        setImageDrawables(images.map {
            AppCompatResources.getDrawable(context, it)!!
        })
    }

    fun setImageBitmaps(images: List<Bitmap>) {
        setImageDrawables(images.map {
            BitmapDrawable(context.resources, it)
        })
    }

    fun setImageDrawables(images: List<Drawable>) {
        this.images.addAll(images)

        if (currentImageIndex < 0) {
            currentImageIndex = 0
            updateCurrentImage()
        }

        setupControls()

        this.imageAdapter.notifyDataSetChanged()
    }

    fun clearImages() {
        currentImage = null
        currentImageIndex = -1
        images.clear()
    }

    private fun next() {
        if (currentImageIndex < images.lastIndex) {
            currentImageIndex++
            updateCurrentImage()
        }
    }

    private fun previous() {
        if (currentImageIndex > 0) {
            currentImageIndex--
            updateCurrentImage()
        }
    }

    private fun updateCurrentImage() {
        currentImage = images[currentImageIndex]
        imageslider_pager.currentItem = currentImageIndex
        updateSliderControls()
    }

    private fun updateSliderControls() {
        if (images.size > 1) {
            ImageViewCompat.setImageTintList(imageslider_previous, ColorStateList.valueOf(activeColor))
            ImageViewCompat.setImageTintList(imageslider_next, ColorStateList.valueOf(activeColor))

            if (currentImageIndex == 0) {
                ImageViewCompat.setImageTintList(imageslider_previous, ColorStateList.valueOf(disabledColor))
            } else if (currentImageIndex == images.lastIndex) {
                ImageViewCompat.setImageTintList(imageslider_next, ColorStateList.valueOf(disabledColor))
            }

            imageslider_indicator.text = "${currentImageIndex + 1}/${images.size}"
        }
    }

    private fun setupControls() {
        if (images.size > 1) {
            imageslider_previous.visibility = View.VISIBLE
            imageslider_next.visibility = View.VISIBLE
            imageslider_indicator.visibility = View.VISIBLE
        } else {
            imageslider_previous.visibility = View.INVISIBLE
            imageslider_next.visibility = View.INVISIBLE
            imageslider_indicator.visibility = View.INVISIBLE
        }
    }

    private inner class ImageAdapter : RecyclerView.Adapter<ImageViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
            val params = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            val image = ImageView(parent.context)

            image.layoutParams = params

            return ImageViewHolder(image)
        }

        override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
            val image = images[position]
            val imageView = holder.itemView as ImageView

            imageView.setImageDrawable(image)
        }

        override fun getItemCount(): Int {
            return images.size
        }
    }

    private class ImageViewHolder(itemView: ImageView) : RecyclerView.ViewHolder(itemView)
}