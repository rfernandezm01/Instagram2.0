package com.example.instagram20;

import android.app.Fragment;
import android.widget.ImageView;
import android.widget.VideoView;

public class MediaFragment extends Fragment {
    VideoView videoView;
    ImageView imageView;

    public AppViewModel appViewModel;
    public MediaFragment() {
// Required empty public constructor
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup
            container, Bundle savedInstanceState)
    {
// Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_media, container, false);
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle
            savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);
        appViewModel = new
                ViewModelProvider(requireActivity()).get(AppViewModel.class);
        imageView = view.findViewById(R.id.imageView);
        videoView = view.findViewById(R.id.videoView);
        appViewModel.postSeleccionado.observe(getViewLifecycleOwner(), post
                -> {
            String mediaType = post.get("mediaType").toString();
            String mediaUrl = post.get("mediaUrl").toString();
            if ("video".equals(mediaType) ||
                    "audio".equals(mediaType)) {
                MediaController mc = new
                12
                MediaController(requireContext());
                mc.setAnchorView(videoView);
                videoView.setMediaController(mc);
                videoView.setVideoPath(post.get("mediaUrl").toString());
                videoView.start();
            } else if ("image".equals(mediaType)) {
                Glide.with(requireView()).load(mediaUrl).into(imageView);
            }
        });
    }
}