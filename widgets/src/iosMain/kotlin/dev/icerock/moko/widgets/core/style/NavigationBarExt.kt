/*
 * Copyright 2020 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */
package dev.icerock.moko.widgets.core.style

import dev.icerock.moko.graphics.toUIColor
import dev.icerock.moko.mvvm.livedata.MutableLiveData
import dev.icerock.moko.widgets.core.associatedObject
import dev.icerock.moko.widgets.core.screen.navigation.NavigationBar
import dev.icerock.moko.widgets.core.utils.toUIBarButtonItem
import dev.icerock.moko.widgets.core.utils.toUIFont
import platform.Foundation.valueForKey
import platform.UIKit.*
import platform.darwin.NSObject

fun UINavigationBar.applyNavigationBarStyle(style: NavigationBar.Styles?) {
    val textAttributes: Map<Any?, *>? = style?.textStyle?.let { ts ->
        val attributes = mutableMapOf<Any?, Any?>()
        ts.color?.also {
            attributes[NSForegroundColorAttributeName] = it.toUIColor()
        }
        ts.toUIFont()?.also {
            attributes[NSFontAttributeName] = it
        }
        attributes
    }
    val backgroundColor = style?.backgroundColor?.toUIColor()
    val tintColor = style?.tintColor?.toUIColor()
        ?: UIApplication.sharedApplication.keyWindow?.rootViewController()?.view?.tintColor!!
    val shadowImage = if (style?.isShadowEnabled == false) UIImage() else null
    val backgroundImage = if (style?.isShadowEnabled == false) UIImage() else null
    val transparent = style?.isTransparent ?: false

//    TODO uncomment when kotlin-native will fix linking to newest api
//    if (UIDevice.currentDevice.systemVersion.compareTo("13.0") < 0) {
    this.barTintColor = backgroundColor
    this.titleTextAttributes = textAttributes
    this.tintColor = tintColor
    this.shadowImage = shadowImage
    this.translucent = transparent
    this.setBackgroundImage(backgroundImage, forBarMetrics = UIBarMetricsDefault)
//    } else {
//        val appearance = UINavigationBarAppearance().apply {
//            configureWithDefaultBackground()
//
//            this.backgroundColor = backgroundColor
//            textAttributes?.let { this.titleTextAttributes = it }
//        }
//
//        this.scrollEdgeAppearance = appearance
//        this.standardAppearance = appearance
//        this.tintColor = tintColor
//    }
}

@Suppress("unused")
fun NavigationBar.None.apply(
    navigationController: UINavigationController?
) {
    navigationController?.navigationBarHidden = true
}

fun NavigationBar.Normal.apply(
    navigationController: UINavigationController?,
    viewController: UIViewController
) {
    navigationController?.navigationBarHidden = false
    viewController.navigationItem.title = title.localized()

    titleImage?.let {
        val imageView = UIImageView(it.toUIImage())

        imageView.contentMode = UIViewContentMode.UIViewContentModeScaleAspectFit
        imageView.translatesAutoresizingMaskIntoConstraints = false

        titleImageWidthAnchor?.toDouble()?.let { widthAnchor ->
            imageView.widthAnchor.constraintEqualToConstant(widthAnchor).active = true
        }

        titleImageHeightAnchor?.toDouble()?.let { heightAnchor ->
            imageView.heightAnchor.constraintEqualToConstant(heightAnchor).active = true
        }

        viewController.navigationItem.titleView = imageView
    }

    navigationController?.navigationBar?.applyNavigationBarStyle(styles)

    backButton?.also {
        viewController.navigationItem.leftBarButtonItem = it.toUIBarButtonItem()
    }

    val rightButtons: List<UIBarButtonItem>? = actions?.map {
        it.toUIBarButtonItem()
    }?.reversed()
    viewController.navigationItem.rightBarButtonItems = rightButtons
}

fun NavigationBar.Search.apply(
    navigationController: UINavigationController?,
    viewController: UIViewController
) {
    navigationController?.navigationBarHidden = false
    viewController.navigationItem.title = title.localized()
    navigationController?.navigationBar?.applyNavigationBarStyle(styles)

    backButton?.also {
        viewController.navigationItem.leftBarButtonItem = it.toUIBarButtonItem()
    }

    val searchController = UISearchController(searchResultsController = null).apply {
        searchResultsUpdater = SearchResultsLiveDataUpdater(searchQuery).also {
            associatedObject = it
        }
        obscuresBackgroundDuringPresentation = false
        searchBar.placeholder = searchPlaceholder?.localized()

        styles?.tintColor?.also { searchBar.tintColor = it.toUIColor() }

        searchBar.searchBarStyle = when (iosSearchBarStyle) {
            NavigationBar.IOSSearchBarStyle.DEFAULT -> UISearchBarStyle.UISearchBarStyleDefault
            NavigationBar.IOSSearchBarStyle.PROMINENT -> UISearchBarStyle.UISearchBarStyleProminent
            NavigationBar.IOSSearchBarStyle.MINIMAL -> UISearchBarStyle.UISearchBarStyleMinimal
        }

        textFieldStyles?.also { style ->
            val textField = searchBar.valueForKey("searchField") as? UITextField

            style.textStyle?.also { ts ->
                ts.color?.also { textField?.textColor = it.toUIColor() }
                ts.fontStyle?.also { textField?.font = ts.toUIFont() }
            }

            viewController.navigationItem.hidesSearchBarWhenScrolling =
                iosHidesSearchBarWhenScrolling

            style.iconTintColor?.also {
                textField?.leftView?.tintColor = it.toUIColor()
            }

            style.backgroundColor?.also {
                textField?.backgroundColor = it.toUIColor()
            }
        }

        setAutomaticallyShowsSearchResultsController(true)
    }

    viewController.navigationItem.hidesSearchBarWhenScrolling = iosHidesSearchBarWhenScrolling
    viewController.navigationItem.searchController = searchController
    viewController.definesPresentationContext = true
}

class SearchResultsLiveDataUpdater(
    private val liveData: MutableLiveData<String>
) : NSObject(), UISearchResultsUpdatingProtocol {

    override fun updateSearchResultsForSearchController(searchController: UISearchController) {
        liveData.value = searchController.searchBar.text.orEmpty()
    }
}
