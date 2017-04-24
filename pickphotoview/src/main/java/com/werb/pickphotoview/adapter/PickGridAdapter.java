package com.werb.pickphotoview.adapter;import android.content.Context;import android.content.Intent;import android.graphics.Bitmap;import android.graphics.drawable.BitmapDrawable;import android.hardware.Camera;import android.net.Uri;import android.os.Build;import android.os.Environment;import android.os.Handler;import android.os.Looper;import android.provider.MediaStore;import android.support.v4.content.FileProvider;import android.support.v7.widget.RecyclerView;import android.util.Log;import android.util.Size;import android.view.LayoutInflater;import android.view.SurfaceView;import android.view.View;import android.view.ViewGroup;import android.view.animation.Animation;import android.view.animation.AnimationUtils;import android.widget.FrameLayout;import android.widget.ImageView;import android.widget.RelativeLayout;import android.widget.Toast;import com.bumptech.glide.Glide;import com.bumptech.glide.RequestManager;import com.bumptech.glide.load.engine.DiskCacheStrategy;import com.werb.pickphotoview.PickPhotoActivity;import com.werb.pickphotoview.R;import com.werb.pickphotoview.util.PickConfig;import com.werb.pickphotoview.util.PickMediaUtils;import com.werb.pickphotoview.util.PickUtils;import java.io.File;import java.io.IOException;import java.lang.ref.WeakReference;import java.text.SimpleDateFormat;import java.util.ArrayList;import java.util.Date;import java.util.List;import java.util.UUID;/** * Created by wanbo on 2016/12/31. */public class PickGridAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {    private List<String> imagePaths;    private boolean isShowCamera;    private int spanCount;    private int maxSelectSize;    private List<String> selectPath;    private PickPhotoActivity context;    private View.OnClickListener imgClick;    private int scaleSize;    private RequestManager manager;    public PickGridAdapter(Context c, RequestManager manager, List<String> imagePaths, boolean isShowCamera, int spanCount, int maxSelectSize, View.OnClickListener imgClick) {        this.context = (PickPhotoActivity) c;        this.manager = manager;        this.imagePaths = imagePaths;        this.isShowCamera = isShowCamera;        this.spanCount = spanCount;        this.maxSelectSize = maxSelectSize;        this.imgClick = imgClick;        selectPath = new ArrayList<>();        buildScaleSize();    }    @Override    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {        if (viewType == PickConfig.CAMERA_TYPE) {            return new CameraViewHolder(LayoutInflater.from(context).inflate(R.layout.pick_item_camera_layout, parent, false));        } else {            return new GridImageViewHolder(LayoutInflater.from(context).inflate(R.layout.pick_item_grid_layout, parent, false));        }    }    @Override    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {        if (holder instanceof GridImageViewHolder) {            String path;            if (isShowCamera) {                path = imagePaths.get(position - 1);            } else {                path = imagePaths.get(position);            }            GridImageViewHolder gridImageViewHolder = (GridImageViewHolder) holder;            gridImageViewHolder.bindItem(path);        }    }    @Override    public int getItemViewType(int position) {        if (isShowCamera) {            if (position == 0) {                return PickConfig.CAMERA_TYPE;            } else {                return position;            }        } else {            return position;        }    }    @Override    public int getItemCount() {        if (isShowCamera) {            return imagePaths.size() + 1;        } else {            return imagePaths.size();        }    }    @Override    public void onViewRecycled(RecyclerView.ViewHolder holder) {        if(holder instanceof GridImageViewHolder) {            GridImageViewHolder gridImageViewHolder = (GridImageViewHolder) holder;            Glide.clear(gridImageViewHolder.weekImage);        }        super.onViewRecycled(holder);    }    public void updateData(List<String> paths) {        imagePaths = paths;        selectPath.clear();        notifyDataSetChanged();    }    // ViewHolder    private class GridImageViewHolder extends RecyclerView.ViewHolder {        private ImageView selectImage, weekImage;        private FrameLayout selectLayout;        GridImageViewHolder(View itemView) {            super(itemView);            ImageView gridImage = (ImageView) itemView.findViewById(R.id.iv_grid);            selectImage = (ImageView) itemView.findViewById(R.id.iv_select);            selectLayout = (FrameLayout) itemView.findViewById(R.id.frame_select_layout);            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) gridImage.getLayoutParams();            params.width = scaleSize;            params.height = scaleSize;            final WeakReference<ImageView> imageViewWeakReference = new WeakReference<>(gridImage);            weekImage = imageViewWeakReference.get();        }        void bindItem(final String path) {            if (selectPath.contains(path)) {                select();            } else {                unSelect();            }            if (weekImage != null) {                handler.post(new Runnable() {                    @Override                    public void run() {                        manager                                .load(Uri.parse("file://" + path))                                .dontAnimate()                                .crossFade()                                .into(weekImage);                    }                });                selectLayout.setTag(R.id.pick_image_path,path);                if(maxSelectSize == 1){                    selectLayout.setOnClickListener(singleClick);                }else {                    selectLayout.setOnClickListener(moreClick);                }                weekImage.setTag(R.id.pick_image_path, path);                weekImage.setOnClickListener(imgClick);            }        }        void select() {            selectImage.setBackgroundDrawable(context.getResources().getDrawable(R.mipmap.pick_ic_select));            selectImage.setTag(R.id.pick_is_select, true);        }        void unSelect() {            selectImage.setBackgroundDrawable(context.getResources().getDrawable(R.mipmap.pick_ic_un_select));            selectImage.setTag(R.id.pick_is_select, false);        }        void addPath(String path) {            selectPath.add(path);            context.updateSelectText(String.valueOf(selectPath.size()));        }        void removePath(String path) {            selectPath.remove(path);            context.updateSelectText(String.valueOf(selectPath.size()));        }        View.OnClickListener moreClick = new View.OnClickListener() {            @Override            public void onClick(View v) {                String path = (String) v.getTag(R.id.pick_image_path);                boolean isSelect = (boolean) selectImage.getTag(R.id.pick_is_select);                if (isSelect) {                    if (selectPath.contains(path)) {                        unSelect();                        removePath(path);                    }                } else {                    if (selectPath.size() < maxSelectSize) {                        if (!selectPath.contains(path)) {                            select();                            addPath(path);                        }                    } else {                        Toast.makeText(context, String.format(context.getString(R.string.pick_photo_size_limit), String.valueOf(maxSelectSize)), Toast.LENGTH_SHORT).show();                    }                }            }        };        View.OnClickListener singleClick = new View.OnClickListener() {            @Override            public void onClick(View v) {                String path = (String) v.getTag(R.id.pick_image_path);                select();                addPath(path);                context.select();            }        };    }    private class CameraViewHolder extends RecyclerView.ViewHolder {        private SurfaceView surfaceView;        CameraViewHolder(View itemView) {            super(itemView);            ViewGroup.LayoutParams params = itemView.getLayoutParams();            params.width = scaleSize;            params.height = scaleSize;            surfaceView = (SurfaceView) itemView.findViewById(R.id.dynamic_view);            new PickMediaUtils().setSurfaceView(surfaceView);            itemView.setOnClickListener(cameraClick);        }        View.OnClickListener cameraClick = new View.OnClickListener() {            @Override            public void onClick(View v) {                try {                    File photoFile = PickUtils.getInstance(context).getPhotoFile();                    if (photoFile.exists()) {                        photoFile.delete();                    }                    if (photoFile.createNewFile()) {                        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);                        intent.putExtra(MediaStore.EXTRA_OUTPUT, PickUtils.getInstance(context).getUri(photoFile));                        context.startActivityForResult(intent, PickConfig.CAMERA_PHOTO_DATA);                    }                } catch (IOException e) {                    e.printStackTrace();                }            }        };    }    private void buildScaleSize() {        int screenWidth = PickUtils.getInstance(context).getWidthPixels();        int space = PickUtils.getInstance(context).dp2px(PickConfig.ITEM_SPACE);        scaleSize = (screenWidth - (spanCount + 1) * space) / spanCount;    }    public List<String> getSelectPath() {        return selectPath;    }    public void setSelectPath(List<String> selectPath){        this.selectPath = selectPath;    }    private Handler handler = new Handler(Looper.getMainLooper());}