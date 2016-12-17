package co.koriel.yonapp.fragment.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import co.koriel.yonapp.R;

public class OneLineBoardAdapter extends BaseAdapter {
    private ArrayList<OneLineBoardItem> oneLineBoardItems;

    public OneLineBoardAdapter() {
        oneLineBoardItems = new ArrayList<>();
    }

    @Override
    public int getCount() {
        return this.oneLineBoardItems.size();
    }

    @Override
    public Object getItem(int i) {
        return this.oneLineBoardItems.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        final int position = i;
        final Context context = viewGroup.getContext();

        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.oneline_listview_item, viewGroup, false);
        }

        final TextView idTextView = (TextView) view.findViewById(R.id.text_id);
        TextView timeBeforeTextView = (TextView) view.findViewById(R.id.text_time);
        TextView contentTextView = (TextView) view.findViewById(R.id.text_content);
        ImageView attachedImageView = (ImageView) view.findViewById(R.id.image_attach);
        TextView attachedTextView = (TextView) view.findViewById(R.id.text_picture_attached);
        final TextView likeCountTextView = (TextView) view.findViewById(R.id.text_like_count);
        final TextView commentCountTextView = (TextView) view.findViewById(R.id.text_comment_count);

        final OneLineBoardItem oneLineBoardItem = this.oneLineBoardItems.get(position);

        final Animation fadeIn = new AlphaAnimation(0.0f, 1.0f);
        fadeIn.setDuration(500);

        final Animation fadeOutIdText = new AlphaAnimation(1.0f, 0.0f);
        fadeOutIdText.setDuration(500);
        fadeOutIdText.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                idTextView.setText(oneLineBoardItem.getId());
                idTextView.startAnimation(fadeIn);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        String preString = idTextView.getText().toString();
        if (!preString.equals(oneLineBoardItem.getId())) {
            if (preString.length() == 0) {
                idTextView.setText(oneLineBoardItem.getId());
                idTextView.startAnimation(fadeIn);
            } else {
                idTextView.startAnimation(fadeOutIdText);
            }
        } else {
            idTextView.setText(oneLineBoardItem.getId());
        }

        timeBeforeTextView.setText(oneLineBoardItem.getTimeBefore());
        contentTextView.setText(oneLineBoardItem.getContent());

        if (oneLineBoardItem.isPicture()) {
            attachedImageView.setVisibility(View.VISIBLE);
            attachedTextView.setVisibility(View.VISIBLE);
        } else {
            attachedImageView.setVisibility(View.INVISIBLE);
            attachedTextView.setVisibility(View.INVISIBLE);
        }

        final Animation fadeOutLikeText = new AlphaAnimation(1.0f, 0.0f);
        fadeOutLikeText.setDuration(500);
        fadeOutLikeText.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                likeCountTextView.setText(Integer.toString(oneLineBoardItem.getLikeCount()));
                likeCountTextView.startAnimation(fadeIn);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        preString = likeCountTextView.getText().toString();
        if (!preString.equals(Integer.toString(oneLineBoardItem.getLikeCount()))) {
            if (preString.length() == 0) {
                likeCountTextView.setText(Integer.toString(oneLineBoardItem.getLikeCount()));
                likeCountTextView.startAnimation(fadeIn);
            } else {
                likeCountTextView.startAnimation(fadeOutLikeText);
            }
        } else {
            likeCountTextView.setText(Integer.toString(oneLineBoardItem.getLikeCount()));
        }

        final Animation fadeOutCommentCountText = new AlphaAnimation(1.0f, 0.0f);
        fadeOutCommentCountText.setDuration(500);
        fadeOutCommentCountText.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                commentCountTextView.setText(Integer.toString(oneLineBoardItem.getCommentCount()));
                commentCountTextView.startAnimation(fadeIn);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        preString = commentCountTextView.getText().toString();
        if (!preString.equals(Integer.toString(oneLineBoardItem.getCommentCount()))) {
            if (preString.length() == 0) {
                commentCountTextView.setText(Integer.toString(oneLineBoardItem.getCommentCount()));
                commentCountTextView.startAnimation(fadeIn);
            } else {
                commentCountTextView.startAnimation(fadeOutCommentCountText);
            }
        } else {
            commentCountTextView.setText(Integer.toString(oneLineBoardItem.getCommentCount()));
        }

        return view;
    }

    public void addItem(OneLineBoardItem oneLineBoardItem) {
        this.oneLineBoardItems.add(oneLineBoardItem);
    }

    public void addItem(int index, OneLineBoardItem oneLineBoardItem) {
        this.oneLineBoardItems.add(index, oneLineBoardItem);
    }

    public void addAllItems(ArrayList<OneLineBoardItem> oneLineBoardItems) {
        this.oneLineBoardItems.addAll(oneLineBoardItems);
    }

    public void addAllItems(int index, ArrayList<OneLineBoardItem> oneLineBoardItems) {
        this.oneLineBoardItems.addAll(index, oneLineBoardItems);
    }

    public void removeItem(int index) {
        this.oneLineBoardItems.remove(index);
    }

    public void clearItems() {
        this.oneLineBoardItems.clear();
    }
}
