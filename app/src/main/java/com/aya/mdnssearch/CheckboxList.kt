package com.aya.mdnssearch

import android.content.Context
import android.util.AttributeSet
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.checkbox.MaterialCheckBox.OnCheckedStateChangedListener

class CheckboxList(context: Context, attrs: AttributeSet? = null)  : LinearLayout(context, attrs),
    CompoundButton.OnCheckedChangeListener {

    private final val limitSize = 5;
    private var checkedList : ArrayList<CompoundButton?> = ArrayList(limitSize)

    init {
        setOrientation(VERTICAL);
        initCheckboxes()
    }

    private fun initCheckboxes() {
        val allValues = ServiceType.values()
        for(v in allValues) {
            initCheckbox(v.name)
        }
    }

    private fun initCheckbox(text: String) {
        var c = CheckBox(context)
        val layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        c.setOnCheckedChangeListener(this)
        c.text = text
        addView(c)
    }

    public fun getCheckboxItem():ArrayList<ServiceType> {
        val array = ArrayList<ServiceType>()
        for(c in checkedList) {
            array.add(ServiceType.valueOf(c?.text.toString()))
        }
        return array
    }

    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
        if (isChecked) {
            if (checkedList.size >= 5) {
                buttonView!!.isChecked = false
                //Todo: need toast notify user
            } else {
                checkedList.add(buttonView)
            }
        } else {
            checkedList.remove(buttonView)
        }
    }
}