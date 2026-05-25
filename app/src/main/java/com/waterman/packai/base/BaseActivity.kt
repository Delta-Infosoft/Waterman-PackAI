package com.waterman.packai.base

import android.content.Context
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.waterman.packai.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
open class BaseActivity : AppCompatActivity() {

    fun loadFragment(fragment : Fragment,isAdd : Boolean, isAddBackStack : Boolean){
        val transaction = supportFragmentManager.beginTransaction()
        if(isAdd){
            transaction.add(/* containerViewId = */ R.id.placeHolder,/* fragment = */fragment,fragment.javaClass.simpleName)
        }else{
            transaction.replace(/* containerViewId = */ R.id.placeHolder,/* fragment = */fragment,fragment.javaClass.simpleName)
        }

        if(isAddBackStack){
            transaction.addToBackStack(fragment.javaClass.simpleName)
        }

        transaction.commit()
    }

    fun Context.showToast(message: String) {
        Toast.makeText(
            this,
            message,
            Toast.LENGTH_LONG
        ).show()
    }

}