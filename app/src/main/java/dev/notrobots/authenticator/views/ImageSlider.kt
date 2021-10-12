package dev.notrobots.authenticator.views

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import dev.notrobots.androidstuff.extensions.resolveColorAttribute
import dev.notrobots.androidstuff.extensions.setTint
import dev.notrobots.authenticator.R
import kotlinx.android.synthetic.main.view_imageslider.view.*

class ImageSlider(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int
) : FrameLayout(context, attrs, defStyleAttr) {
    private var images = mutableListOf<Drawable>()
    private val imageAdapter = ImageAdapter()
    private var callback: Callback? = null
    private var useDefaultControls = true
    var infiniteScroll: Boolean = false //TODO: Create attributes for these properties
    var currentImage: Drawable? = null
        private set
    var currentImageIndex: Int = -1
        private set
    var indicatorView: View
        private set
    var nextView: View? = null
        set(value) {
            field?.visibility = View.INVISIBLE
            field = value
            field!!.visibility = View.VISIBLE
            field!!.setOnClickListener { next() }
            useDefaultControls = false
        }
    var previousView: View? = null
        set(value) {
            field?.visibility = View.INVISIBLE
            field = value
            field!!.visibility = View.VISIBLE
            field!!.setOnClickListener { previous() }
            useDefaultControls = false
        }

    init {
        inflate(context, R.layout.view_imageslider, this)

        nextView = imageslider_next
        previousView = imageslider_previous
        indicatorView = imageslider_indicator
        useDefaultControls = true

        imageslider_pager.adapter = imageAdapter
        imageslider_pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)

                notifyImageChanged(currentImageIndex, position)
                currentImageIndex = position
            }
        })
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
        this.images = images.toMutableList()

        if (images.isNotEmpty()) {
            currentImageIndex = 0
            currentImage = images[0]
            imageAdapter.notifyDataSetChanged()
            imageslider_pager.currentItem = 0
        }
    }

    fun clearImages() {
        currentImage = null
        currentImageIndex = -1
        images.clear()
    }

    fun getImage(position: Int): Drawable {
        return images[position]
    }

    fun next() {
        val oldPosition = currentImageIndex

        if (infiniteScroll) {
            currentImageIndex = (currentImageIndex + 1) % images.size
            notifyImageChanged(oldPosition, currentImageIndex)
        } else if (currentImageIndex < images.lastIndex) {
            currentImageIndex++
            notifyImageChanged(oldPosition, currentImageIndex)
        }
    }

    fun previous() {
        val oldPosition = currentImageIndex

        if (infiniteScroll) {
            currentImageIndex = if (currentImageIndex == 0) images.lastIndex else currentImageIndex - 1
            notifyImageChanged(oldPosition, currentImageIndex)
        } else if (currentImageIndex > 0) {
            currentImageIndex--
            notifyImageChanged(oldPosition, currentImageIndex)
        }
    }

    fun setCallback(callback: Callback) {
        this.callback = callback
    }

    private fun notifyImageChanged(oldIndex: Int, newIndex: Int) {
        currentImage = images[newIndex]
        imageslider_pager.currentItem = newIndex

        if (oldIndex < newIndex) {
            callback?.onNextImage(nextView!!, newIndex)
        } else if (oldIndex > newIndex) {
            callback?.onPreviousImage(previousView!!, newIndex)
        }
        callback?.onImageChanged(this, oldIndex, newIndex)

        if (useDefaultControls) {
            val activeColor = context.resolveColorAttribute(R.attr.colorPrimary)
            val disabledColor = Color.LTGRAY

            (nextView as ImageView).setTint(disabledColor)
            (previousView as ImageView).setTint(disabledColor)

            if (newIndex > 0) {
                (previousView as ImageView).setTint(activeColor)
            }

            if (newIndex < images.lastIndex) {
                (nextView as ImageView).setTint(activeColor)
            }
        }

        imageslider_indicator.text = "${newIndex + 1}/${images.size}"
    }

    interface Callback {
        fun onNextImage(view: View, position: Int)
        fun onPreviousImage(view: View, position: Int)
        fun onImageChanged(view: ImageSlider, old: Int, new: Int)
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