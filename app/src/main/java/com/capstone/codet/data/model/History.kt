package com.capstone.codet.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class History (

    var title:String,
    var desc:String,
    var date:String,
    var img: Int = 0

):Parcelable