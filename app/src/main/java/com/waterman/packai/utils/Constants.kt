package com.waterman.packai.utils

import android.content.Context
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.graphics.Typeface
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Build.VERSION_CODES
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.content.ContextCompat
import com.google.android.material.textfield.TextInputEditText
import com.waterman.packai.R
import com.waterman.packai.databinding.CommonEditDialogBinding
import com.waterman.packai.network.response.BrandList
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object Constants {

    fun hideKeyboard(view: View?) {
        view ?: return
        val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    fun getDeviceId(context: Context): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: ""
    }

    fun AppCompatEditText.isEmpty(): Boolean {
        return this.text.toString().trim().isEmpty()
    }

    fun TextInputEditText.isEmpty(): Boolean = this.text.toString().trim().isEmpty()
    fun TextInputEditText.getTrimmedText(): String = this.text?.toString()?.trim().orEmpty()

    fun TextInputEditText.doubleValue(): Double =
        this.text?.toString()?.trim().orEmpty().toDoubleOrNull() ?: 0.0

    fun AppCompatEditText.getTrimmedText(): String {
        return this.text.toString().trim()
    }

    //This for Check Newtwork
    fun isInternetAvailable(context: Context): Boolean {
        try {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            if (Build.VERSION.SDK_INT >= VERSION_CODES.M) {
                val network = connectivityManager.activeNetwork ?: return false
                val networkCapabilities =
                    connectivityManager.getNetworkCapabilities(network) ?: return false

                val hasInternet =
                    networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)

                // Check if the network is validated (i.e., actually connected to the internet)
                val isNetworkValidated =
                    networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

                // For Wi-Fi, ensure it's validated before returning true
                if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    val hasWiFiInternet =
                        networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                    return hasInternet && hasWiFiInternet
                }

                return hasInternet && isNetworkValidated

            } else {
                @Suppress("DEPRECATION")
                val activeNetworkInfo = connectivityManager.activeNetworkInfo
                return activeNetworkInfo?.isConnected == true
            }
        } catch (e: Exception) {
            Log.e("InternetCheck", "Error checking internet connection: ${e.localizedMessage}")
            return false
        }
    }

    fun View.setSafeOnClickListener(
        interval: Long = 800L,
        onSafeClick: (View) -> Unit
    ) {
        var lastClickTime = 0L

        setOnClickListener { view ->
            val currentTime = SystemClock.elapsedRealtime()
            if (currentTime - lastClickTime >= interval) {
                lastClickTime = currentTime
                onSafeClick(view)
            }
        }
    }


    fun getCurrentTimestamp(dateTimeFormat: String): String {
        return try {
            val dateFormat = SimpleDateFormat(dateTimeFormat, Locale.ENGLISH)
            dateFormat.format(Date())
        } catch (e: Exception) {
            ""
        }
    }

    fun getDeviceName(): String {
        val deviceName = (Build.MANUFACTURER
                + " " + Build.MODEL
                + " " + Build.VERSION.RELEASE
                + " " + VERSION_CODES::class.java.getFields()[Build.VERSION.SDK_INT].getName())

        return deviceName
    }

    fun getAndroidVersion(): String {
        var version = ""
        val builder = kotlin.text.StringBuilder()
        builder.append("android : ").append(Build.VERSION.RELEASE)

        val fields = VERSION_CODES::class.java.getFields()
        for (field in fields) {
            val fieldName = field.getName()
            var fieldValue = -1

            try {
                fieldValue = field.getInt(Any())
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
            } catch (e: IllegalAccessException) {
                e.printStackTrace()
            } catch (e: NullPointerException) {
                e.printStackTrace()
            }

            if (fieldValue == Build.VERSION.SDK_INT) {
                builder.append(" : ").append(fieldName).append(" : ")
                builder.append("sdk=").append(fieldValue)
            }
        }

        version = builder.toString()

        return version
    }

    fun isGpsEnabled(context: Context): Boolean {
        val locationManager =
            context.applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    fun getBatteryLevel(context: Context): Int {
        val batteryManager =
            context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager

        return batteryManager.getIntProperty(
            BatteryManager.BATTERY_PROPERTY_CAPACITY
        )
    }

    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val network = connectivityManager.activeNetwork ?: return false
        val networkCapabilities =
            connectivityManager.getNetworkCapabilities(network) ?: return false

        return networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    fun getAppVersion(context: Context): String {
        return try {
            context.packageManager
                .getPackageInfo(context.packageName, 0)
                .versionName ?: "N/A"
        } catch (e: Exception) {
            "N/A"
        }
    }

    fun isSameAsToday(inTime: String?): Boolean {
        if (inTime.isNullOrBlank()) return false

        val formats = listOf(
            "dd/MM/yyyy hh:mm:ss a",
            "d/M/yyyy hh:mm:ss a",
            "MM/dd/yyyy hh:mm:ss a",
            "M/d/yyyy h:mm:ss a",
            "dd-MM-yyyy HH:mm:ss",
        )

        for (pattern in formats) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.ENGLISH).apply {
                    isLenient = false
                }

                val parsedDate = sdf.parse(inTime) ?: continue

                val apiCal = Calendar.getInstance().apply {
                    time = parsedDate
                }

                val todayCal = Calendar.getInstance()

                if (
                    apiCal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
                    apiCal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR)
                ) {
                    return true
                }
            } catch (_: Exception) {
                // try next format
            }
        }

        return false
    }

    fun getTodayDateFormatted(inTime: String?): String? {
        if (inTime.isNullOrBlank()) return null

        val inputFormats = listOf(
            "dd/MM/yyyy hh:mm:ss a",
            "d/M/yyyy hh:mm:ss a",
            "MM/dd/yyyy hh:mm:ss a",
            "M/d/yyyy h:mm:ss a",
            "dd-MM-yyyy HH:mm:ss",
        )

        val outputFormat = SimpleDateFormat(
            "dd/MM/yyyy hh:mm:ss a",
            Locale.ENGLISH
        )

        for (pattern in inputFormats) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.ENGLISH).apply {
                    isLenient = false
                }

                val parsedDate = sdf.parse(inTime) ?: continue

                val apiCal = Calendar.getInstance().apply { time = parsedDate }
                val todayCal = Calendar.getInstance()

                val isToday =
                    apiCal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
                            apiCal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR)

                if (isToday) {
                    return outputFormat.format(parsedDate)
                }

            } catch (_: Exception) {
                // try next format
            }
        }

        return null
    }

    fun isValidIp(ip: String): Boolean {
        val regex =
            "^(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}$"

        if (!ip.matches(Regex(regex))) return false

        val parts = ip.split(".").map { it.toInt() }

        // 0.0.0.0
        if (parts.all { it == 0 }) return false

        // Loopback
        if (parts[0] == 127) return false

        // Network / broadcast address
        if (parts[3] == 0 || parts[3] == 255) return false

        return true
    }

    fun showConfirmDialog(
        context: Context,
        title: String,
        message: String? = null,
        okText: String = "Confirm",
        cancelText: String = "Cancel",
        onOk: () -> Unit,
        onCancel: (() -> Unit)? = null
    ) {
        val dialog = AlertDialog.Builder(context)
            .setTitle(title)
            .apply {
                message?.let { setMessage(it) }
            }
            .setPositiveButton(okText) { dialog, _ ->
                dialog.dismiss()
                onOk()
            }
            .setNegativeButton(cancelText) { dialog, _ ->
                dialog.dismiss()
                onCancel?.invoke()
            }
            .setCancelable(false)
            .show()

        dialog.window?.setBackgroundDrawableResource(R.drawable.bg_white_rounded_18)
        val titleId = context.resources.getIdentifier("alertTitle", "id", "android")
        val titleView = dialog.findViewById<TextView>(titleId)
        titleView?.applyGradient()
    }

    fun showSerialMismatchDialog(
        context: Context,
        title: String,
        ierpSrNo: String,
        scannedSrNo: String,
        status: String?, // "OK" or "FAILED"
        mismatchMessage: String,
        okText: String = "Force match",
        cancelText: String = "Cancel",
        onConfirm: (checkedValue: String) -> Unit,
        onCancel: (() -> Unit)? = null
    ) {

        val dialog = AlertDialog.Builder(context).create()
        val binding = CommonEditDialogBinding.inflate(LayoutInflater.from(context))
        dialog.setView(binding.root)
        dialog.setCancelable(false)

        binding.txtTitle.text = title

        when (title) {
            "Pump Sr.No" -> binding.txtIerpSrNo.text = "iERP SR.No: $ierpSrNo"
            "Motor Sr.No" -> binding.txtIerpSrNo.text = "iERP SR.No: $ierpSrNo"
            "Pump Type No" -> binding.txtIerpSrNo.text = "iERP Item Name: $ierpSrNo"
            "Motor Type No" -> binding.txtIerpSrNo.text = "iERP Item Name: $ierpSrNo"
            "Logo" -> binding.txtIerpSrNo.text = "iERP Customer Name: $ierpSrNo"
        }

        binding.edtCheckedSrNo.setText(scannedSrNo)

        binding.txtMismatch.text = mismatchMessage
        binding.txtMismatch.visibility = View.VISIBLE

        binding.btnConfirm.text = okText
        binding.btnCancel.text = cancelText

        if (status.equals("OK", ignoreCase = true)) {
            binding.txtMismatch.setTextColor(ContextCompat.getColor(context, R.color.green_1))

            binding.btnConfirm.visibility = View.GONE
            binding.edtCheckedSrNo.isEnabled = false
            binding.btnCancel.visibility = View.VISIBLE

        } else {
            binding.txtMismatch.setTextColor(ContextCompat.getColor(context, R.color.red_1))

            binding.btnConfirm.visibility = View.VISIBLE
            if (title == "Logo") {
                binding.btnConfirm.visibility = View.GONE
                binding.edtCheckedSrNo.isEnabled = false
            }
            binding.btnCancel.visibility = View.VISIBLE
        }

        binding.btnCancel.setOnClickListener {
            dialog.dismiss()
            onCancel?.invoke()
        }

        binding.btnConfirm.setOnClickListener {
            val checkedValue = binding.edtCheckedSrNo.text.toString().trim()

            if (checkedValue.isEmpty()) {
                binding.txtLayCheckedSrNo.error = "Please enter SR No"
                return@setOnClickListener
            }

            dialog.dismiss()
            onConfirm(checkedValue)
        }

        dialog.show()
        dialog.window?.setBackgroundDrawableResource(R.drawable.bg_white_rounded_18)
    }

    fun TextView.applyGradient() {
        val width = paint.measureText(text.toString())
        val shader = LinearGradient(
            0f, 0f, width, textSize,
            intArrayOf(
                Color.parseColor("#48CFD6"),  // Start color
                Color.parseColor("#CE82E7")  // End color
            ),
            null,
            Shader.TileMode.CLAMP
        )
        paint.shader = shader
        invalidate()
    }

    fun showBrandSelectionDialog(
        context: Context,
        brandList: List<BrandList>,
        onBrandSelected: (BrandList) -> Unit
    ) {

        val brandNames = brandList.map { it.Text ?: "" }.toTypedArray()

        val dialog = AlertDialog.Builder(context)
            .setTitle("Select Brand")
            .setItems(brandNames) { dialogInterface, which ->

                val selectedBrand = brandList[which]

                dialogInterface.dismiss()
                onBrandSelected(selectedBrand)
            }
            .setCancelable(false)
            .show()

        dialog.window?.setBackgroundDrawableResource(R.drawable.bg_white_rounded_18)

        val titleId = context.resources.getIdentifier("alertTitle", "id", "android")
        val titleView = dialog.findViewById<TextView>(titleId)

        titleView?.apply {
            setTypeface(typeface, Typeface.BOLD)
            applyGradient()
        }
    }

   /* fun showBrandSelectionDialog(context: Context, onBrandSelected: (String) -> Unit) {
        val brands = arrayOf("KSB", "WILLO", "WATERMAN")

        val dialog = AlertDialog.Builder(context)
            .setTitle("Select Brand")
            .setItems(brands) { dialogInterface, which ->

                val selectedBrand = brands[which]
                dialogInterface.dismiss()
                onBrandSelected(selectedBrand)
            }
            .setCancelable(false)
            .show()

        // ✅ Same background
        dialog.window?.setBackgroundDrawableResource(R.drawable.bg_white_rounded_18)

        // ✅ Make title gradient + bold
        val titleId = context.resources.getIdentifier("alertTitle", "id", "android")
        val titleView = dialog.findViewById<TextView>(titleId)
        titleView?.apply {
            setTypeface(typeface, Typeface.BOLD)   // Bold
            applyGradient()                        // Your extension
        }
    }*/


}