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
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import dev.notrobots.androidstuff.extensions.hide
import dev.notrobots.androidstuff.extensions.resolveColorAttribute
import dev.notrobots.androidstuff.extensions.setTint
import dev.notrobots.androidstuff.extensions.viewBindings
import dev.notrobots.authenticator.R
import dev.notrobots.authenticator.databinding.ViewImagesliderBinding
import dev.notrobots.authenticator.extensions.getResourceIdOrNull
import kotlinx.android.synthetic.main.view_imageslider.view.*

class ImageSlider(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int
) : FrameLayout(context, attrs, defStyleAttr) {
    private var images = mutableListOf<Drawable>()
    private val imageAdapter = ImageAdapter()
    private var callback: Callback? = null
    private val binding by viewBindings<ViewImagesliderBinding>()

    /**
     * Current selected image or null if there are no images in this slider
     */
    var currentImage: Drawable? = null
        private set

    /**
     * Index of the current selected image or -1 if there
     */
    var currentImageIndex: Int = -1
        private set

    /**
     * Whether or not the scroll is circular
     */
    var infiniteScroll: Boolean = false

    /**
     * Whether or not you want this view to handle [nextView], [previousView] and [indicatorView] internally
     *
     * The ImageSlider will tint the [nextView] and [previousView] using colorControlNormal and colorPrimary.
     * Meanwhile the [indicatorView] will be handled as a [TextView] and will show `{currentImage}/{imageCount}`
     */
    var useDefaultControls = true

    /**
     * The view used to slide to the next image
     *
     * By default it's an [ImageView] to the right of the current image
     */
    var nextView: View? = null
        set(value) {
            field?.visibility = View.INVISIBLE
            field = value
            field?.visibility = View.VISIBLE
            field?.setOnClickListener { next() }
        }

    /**
     * The view used to slide to the previous image
     *
     * By default it's an [ImageView] to the left of the current image
     */
    var previousView: View? = null
        set(value) {
            field?.visibility = View.INVISIBLE
            field = value
            field?.visibility = View.VISIBLE
            field?.setOnClickListener { previous() }
        }

    /**
     * The TextView showing the index of the current selected image
     */
    var indicatorView: TextView? = null
        set(value) {
            field?.visibility = View.INVISIBLE
            field = value
            field?.visibility = View.VISIBLE
        }

    init {
        addView(binding.root)

        nextView = binding.next
        previousView = binding.previous
        indicatorView = binding.indicator
        binding.pager.adapter = imageAdapter
        binding.pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)

                notifyImageChanged(currentImageIndex, position)
                currentImageIndex = position
            }
        })

        with(context.obtainStyledAttributes(attrs, R.styleable.ImageSlider, defStyleAttr, 0)) {
            infiniteScroll = getBoolean(R.styleable.ImageSlider_slider_infiniteScroll, infiniteScroll)
            useDefaultControls = getBoolean(R.styleable.ImageSlider_slider_useDefaultControls, useDefaultControls)

            val nextViewId = getResourceIdOrNull(R.styleable.ImageSlider_slider_nextView)
            val previousViewId = getResourceIdOrNull(R.styleable.ImageSlider_slider_previousView)
            val indicatorViewId = getResourceIdOrNull(R.styleable.ImageSlider_slider_indicatorView)

            nextViewId?.let {
                nextView = findViewById(nextViewId)
            }
            previousViewId?.let {
                previousView = findViewById(previousViewId)
            }
            indicatorViewId?.let {
                indicatorView = findViewById(indicatorViewId)
            }
        }
    }

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context) : this(context, null, 0)

    /**
     * Sets the images shown by this slider
     */
    fun setImageResources(images: List<Int>) {
        setImageDrawables(images.map {
            AppCompatResources.getDrawable(context, it)!!
        })
    }

    /**
     * Sets the images shown by this slider
     */
    fun setImageBitmaps(images: List<Bitmap>) {
        setImageDrawables(images.map {
            BitmapDrawable(context.resources, it)
        })
    }

    /**
     * Sets the images shown by this slider
     */
    fun setImageDrawables(images: List<Drawable>) {
        this.images = images.toMutableList()

        if (images.isNotEmpty()) {
            currentImageIndex = 0
            currentImage = images[0]
            imageAdapter.notifyDataSetChanged()
            binding.pager.currentItem = 0
        }
    }

    /**
     * Removes all images from this slider
     */
    fun clearImages() {
        currentImage = null
        currentImageIndex = -1
        images.clear()
    }

    /**
     * Returns the image at the given [position]
     */
    fun getImage(position: Int): Drawable {
        return images[position]
    }

    /**
     * Slides to the next image
     */
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

    /**
     * Slides to the previous image
     */
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

    /**
     * Sets the slider callback
     */
    fun setCallback(callback: Callback) {
        this.callback = callback
    }

    /**
     * Notifies that the current image has changed from [oldIndex] to [newIndex]
     */
    private fun notifyImageChanged(oldIndex: Int, newIndex: Int) {
        currentImage = images[newIndex]
        binding.pager.currentItem = newIndex

        if (oldIndex < newIndex) {
            callback?.onNextImage(nextView!!, newIndex)
        } else if (oldIndex > newIndex) {
            callback?.onPreviousImage(previousView!!, newIndex)
        }

        callback?.onImageChanged(this, oldIndex, newIndex)

        if (useDefaultControls) {
            val activeColor = context.resolveColorAttribute(R.attr.colorPrimary)
            val disabledColor = context.resolveColorAttribute(R.attr.colorControlNormal)

            if (infiniteScroll) {
                (nextView as? ImageView)?.setTint(activeColor)
                (previousView as? ImageView)?.setTint(activeColor)
            } else {
                (nextView as? ImageView)?.setTint(disabledColor)
                (previousView as? ImageView)?.setTint(disabledColor)

                if (newIndex > 0) {
                    (previousView as? ImageView)?.setTint(activeColor)
                }

                if (newIndex < images.lastIndex) {
                    (nextView as? ImageView)?.setTint(activeColor)
                }
            }

            (indicatorView as? TextView)?.text = "${newIndex + 1}/${images.size}"
        }
    }

    interface Callback {
        /**
         * Invoked when the slider changes to the next image
         */
        fun onNextImage(view: View, position: Int)

        /**
         * Invoked when the slider changes to the previous image
         */
        fun onPreviousImage(view: View, position: Int)

        /**
         * Invoked when the slider changes images from the [old] position to the [new] position
         */
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