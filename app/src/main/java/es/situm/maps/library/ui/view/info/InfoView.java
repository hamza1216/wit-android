package es.situm.maps.library.ui.view.info;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

import com.waterloo.wit.R;

public class InfoView extends RelativeLayout {
    public InfoView(Context context) {
        super(context);
    }

    public InfoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray typedArray = context.getTheme().obtainStyledAttributes(attrs, R.styleable.situm_InfoView, 0, 0);
        typedArray.recycle();
    }
}

