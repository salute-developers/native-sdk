package ru.sberdevices.sdk.demoapp.ui.gestures.controller

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import ru.sberdevices.services.pub.demoapp.R

internal class GridAdapter(context: Context) : ArrayAdapter<Tile>(context, 0) {

    fun submitGrid(tileGrid: List<List<Tile>>) {
        clear()
        addAll(tileGrid.flatten())
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.tile_view, parent, false)
        val tile = getItem(position) as Tile
        val colorInt = context.resources.getColor(tile.tileColor, context.theme)
        view.findViewById<View>(R.id.tile_colorview).setBackgroundColor(colorInt)

        return view
    }
}
