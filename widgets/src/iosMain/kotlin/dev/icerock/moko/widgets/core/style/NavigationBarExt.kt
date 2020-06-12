/*
 * Copyright 2020 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */
package dev.icerock.moko.widgets.core.style

import dev.icerock.moko.graphics.toUIColor
import dev.icerock.moko.mvvm.livedata.MutableLiveData
import dev.icerock.moko.widgets.core.objc.setAssociatedObject
import dev.icerock.moko.widgets.core.screen.navigation.NavigationBar
import dev.icerock.moko.widgets.core.utils.toUIBarButtonItem
import dev.icerock.moko.widgets.core.utils.toUIFont
import platform.Foundation.valueForKey
import platform.UIKit.NSFontAttributeName
import platform.UIKit.NSForegroundColorAttributeName
import platform.UIKit.UIApplication
import platform.UIKit.UIBarButtonItem
import platform.UIKit.UIBarMetricsDefault
import platform.UIKit.UIImage
import platform.UIKit.UINavigationBar
import platform.UIKit.UINavigationController
import platform.UIKit.UISearchBarStyle
import platform.UIKit.UISearchController
import platform.UIKit.UISearchResultsUpdatingProtocol
import platform.UIKit.UITextField
import platform.UIKit.UIViewController
import platform.UIKit.backgroundColor
import platform.UIKit.navigationItem
import platform.UIKit.searchTextField
import platform.UIKit.tintColor
import platform.darwin.NSObject

fun UINavigationBar.applyNavigationBarStyle(style: NavigationBar.Styles?) {
    val textAttributes: Map<Any?, *>? = style?.textStyle?.let { ts ->
        val attributes = mutableMapOf<Any?, Any?>()
        if (ts.color != null) {
            attributes[NSForegroundColorAttributeName] = ts.color.toUIColor()
        }
        val font = ts.toUIFont()
        if (font != null) {
            attributes[NSFontAttributeName] = font
        }
        attributes
    }
    val backgroundColor = style?.backgroundColor?.toUIColor()
    val tintColor = style?.tintColor?.toUIColor()
        ?: UIApplication.sharedApplication.keyWindow?.rootViewController()?.view?.tintColor!!
    val shadowImage = if (style?.isShadowEnabled == false) UIImage() else null
    val backgroundImage = if (style?.isShadowEnabled == false) UIImage() else null

//    TODO uncomment when kotlin-native will fix linking to newest api
//    if (UIDevice.currentDevice.systemVersion.compareTo("13.0") < 0) {
    this.barTintColor = backgroundColor
    this.titleTextAttributes = textAttributes
    this.tintColor = tintColor
    this.shadowImage = shadowImage
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
    navigationController?.navigationBar?.applyNavigationBarStyle(styles)

    if (backButton != null) {
        viewController.navigationItem.leftBarButtonItem = backButton.toUIBarButtonItem()
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

    if (backButton != null) {
        viewController.navigationItem.leftBarButtonItem = backButton.toUIBarButtonItem()
    }

    val searchController = UISearchController(searchResultsController = null).apply {
        searchResultsUpdater = SearchResultsLiveDataUpdater(searchQuery).also {
            setAssociatedObject(this, it)
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

            style.iconTintColor?.also {
                textField?.leftView?.tintColor = it.toUIColor()
            }

            style.backgroundColor?.also {
                textField?.backgroundColor = it.toUIColor()
            }
        }

        setAutomaticallyShowsSearchResultsController(true)
    }

    viewController.navigationItem.hidesSearchBarWhenScrolling = false
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
