// Paquete y imports...
package com.example.instagram20;

//import static android.os.Build.ID;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.appwrite.Client;
import io.appwrite.ID;
import io.appwrite.Query;
import io.appwrite.coroutines.CoroutineCallback;
import io.appwrite.exceptions.AppwriteException;
import io.appwrite.models.DocumentList;
import io.appwrite.services.Account;
import io.appwrite.services.Databases;

public class HomeFragment extends Fragment {

    NavController navController;
    AppViewModel appViewModel;
    ImageView photoImageView;
    TextView displayNameTextView, emailTextView;
    Client client;
    Account account;
    String userId;
    PostsAdapter adapter;

    public HomeFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        NavigationView navigationView = view.getRootView().findViewById(R.id.nav_view);
        View header = navigationView.getHeaderView(0);
        photoImageView = header.findViewById(R.id.imageView);
        displayNameTextView = header.findViewById(R.id.displayNameTextView);
        emailTextView = header.findViewById(R.id.emailTextView);

        appViewModel = new ViewModelProvider(requireActivity()).get(AppViewModel.class);
        navController = Navigation.findNavController(view);

        client = new Client(requireContext()).setProject(getString(R.string.APPWRITE_PROJECT_ID));
        account = new Account(client);
        Handler mainHandler = new Handler(Looper.getMainLooper());

        try {
            account.get(new CoroutineCallback<>((result, error) -> {
                if (error != null) {
                    error.printStackTrace();
                    return;
                }
                mainHandler.post(() -> {
                    userId = result.getId();
                    displayNameTextView.setText(result.getName());
                    emailTextView.setText(result.getEmail());
                    Glide.with(requireView()).load(R.drawable.ic_launcher_background).into(photoImageView);
                    obtenerPosts();
                });
            }));
        } catch (AppwriteException e) {
            throw new RuntimeException(e);
        }

        view.findViewById(R.id.gotoNewPostFragmentButton).setOnClickListener(v -> navController.navigate(R.id.newPostFragment));

        RecyclerView postsRecyclerView = view.findViewById(R.id.postsRecyclerView);
        adapter = new PostsAdapter(requireActivity());
        postsRecyclerView.setAdapter(adapter);
    }

    class PostViewHolder extends RecyclerView.ViewHolder {
        ImageView authorPhotoImageView, likeImageView, mediaImageView;
        TextView authorTextView, contentTextView, numLikesTextView, timeTextView;
        Button deletebutton, sharebutton, forwardbutton;

        PostViewHolder(@NonNull View itemView) {
            super(itemView);
            authorPhotoImageView = itemView.findViewById(R.id.authorPhotoImageView);
            authorTextView = itemView.findViewById(R.id.authorTextView);
            mediaImageView = itemView.findViewById(R.id.mediaImage);
            contentTextView = itemView.findViewById(R.id.contentTextView);
            likeImageView = itemView.findViewById(R.id.likeImageView);
            numLikesTextView = itemView.findViewById(R.id.numLikesTextView);
            timeTextView = itemView.findViewById(R.id.timeTextView);
            deletebutton = itemView.findViewById(R.id.deleteButton);
            sharebutton = itemView.findViewById(R.id.shareButton);
            forwardbutton = itemView.findViewById(R.id.forwardButton);
        }
    }

    class PostsAdapter extends RecyclerView.Adapter<PostViewHolder> {
        DocumentList<Map<String, Object>> lista = null;
        private final Activity context;

        PostsAdapter(Activity context) {
            this.context = context;
        }

        @NonNull
        @Override
        public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new PostViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.viewholder_post, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
            Map<String, Object> post = lista.getDocuments().get(position).getData();

            if (post.get("authorPhotoUrl") == null) {
                holder.authorPhotoImageView.setImageResource(R.drawable.user);
            } else {
                Glide.with(getContext()).load(post.get("authorPhotoUrl").toString()).circleCrop().into(holder.authorPhotoImageView);
            }

            holder.authorTextView.setText(post.get("author").toString());
            holder.contentTextView.setText(post.get("content").toString());

            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm");
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(post.get("time") != null ? (long) post.get("time") : 0);
            holder.timeTextView.setText(formatter.format(calendar.getTime()));

            // Likes
            List<String> likes = (List<String>) post.get("likes");
            if (likes.contains(userId)) {
                holder.likeImageView.setImageResource(R.drawable.like_on);
            } else {
                holder.likeImageView.setImageResource(R.drawable.like_off);
            }
            holder.numLikesTextView.setText(String.valueOf(likes.size()));

            holder.likeImageView.setOnClickListener(view -> {
                Databases databases = new Databases(client);
                Handler mainHandler = new Handler(Looper.getMainLooper());

                List<String> nuevosLikes = new ArrayList<>(likes);
                if (nuevosLikes.contains(userId)) {
                    nuevosLikes.remove(userId);
                } else {
                    nuevosLikes.add(userId);
                }

                Map<String, Object> data = new HashMap<>();
                data.put("likes", nuevosLikes);

                try {
                    databases.updateDocument(
                            getString(R.string.APPWRITE_DATABASE_ID),
                            getString(R.string.APPWRITE_POSTS_COLLECTION_ID),
                            post.get("$id").toString(),
                            data,
                            new ArrayList<>(),
                            new CoroutineCallback<>((result, error) -> {
                                if (error != null) {
                                    error.printStackTrace();
                                    return;
                                }
                                mainHandler.post(() -> obtenerPosts());
                            })
                    );
                } catch (AppwriteException e) {
                    throw new RuntimeException(e);
                }
            });

            // Media
            if (post.get("mediaUrl") != null) {
                holder.mediaImageView.setVisibility(View.VISIBLE);
                if ("audio".equals(post.get("mediaType").toString())) {
                    Glide.with(requireView()).load(R.drawable.audio).centerCrop().into(holder.mediaImageView);
                } else {
                    Glide.with(requireView()).load(post.get("mediaUrl").toString()).centerCrop().into(holder.mediaImageView);
                }
                holder.mediaImageView.setOnClickListener(view -> {
                    appViewModel.postSeleccionado.setValue(post);
                    navController.navigate(R.id.mediaFragment);
                });
            } else {
                holder.mediaImageView.setVisibility(View.GONE);
            }

            // Botones
            holder.deletebutton.setVisibility(post.get("uid") != null && post.get("uid").toString().equals(userId) ? View.VISIBLE : View.GONE);

            holder.deletebutton.setOnClickListener(view -> {
                Databases databases = new Databases(client);
                Handler mainHandler = new Handler(Looper.getMainLooper());

                try {
                    databases.deleteDocument(
                            getString(R.string.APPWRITE_DATABASE_ID),
                            getString(R.string.APPWRITE_POSTS_COLLECTION_ID),
                            post.get("$id").toString(),
                            new CoroutineCallback<>((result, error) -> {
                                if (error != null) {
                                    Snackbar.make(view, "Error al eliminar el post", Snackbar.LENGTH_LONG).show();
                                    return;
                                }
                                mainHandler.post(() -> {
                                    obtenerPosts();
                                    Snackbar.make(view, "Post eliminado", Snackbar.LENGTH_LONG).show();
                                });
                            })
                    );
                } catch (Exception e) {
                    Snackbar.make(view, "Error inesperado al eliminar", Snackbar.LENGTH_LONG).show();
                }
            });

            holder.sharebutton.setVisibility(View.VISIBLE);
            holder.sharebutton.setOnClickListener(view -> {
                try {
                    String postContent = post.get("content").toString();
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("text/plain");
                    shareIntent.putExtra(Intent.EXTRA_TEXT, postContent);
                    context.startActivity(Intent.createChooser(shareIntent, "Compartir post vía"));
                } catch (Exception e) {
                    Snackbar.make(view, "Error al compartir", Snackbar.LENGTH_LONG).show();
                }
            });

            holder.forwardbutton.setVisibility(View.VISIBLE);
            holder.forwardbutton.setOnClickListener(view -> {
                Handler mainHandler = new Handler(Looper.getMainLooper());
                Databases databases = new Databases(client);

                Map<String, Object> newPost = new HashMap<>();
                newPost.put("content", post.get("content"));
                newPost.put("author", post.get("author"));
                newPost.put("time", System.currentTimeMillis());
                newPost.put("likes", new ArrayList<String>());

                try {
                    databases.createDocument(
                            getString(R.string.APPWRITE_DATABASE_ID),
                            getString(R.string.APPWRITE_POSTS_COLLECTION_ID),
                            "unique()",
                            newPost,
                            new CoroutineCallback<>((result, error) -> {
                                if (error != null) {
                                    Snackbar.make(view, "Error al reenviar el post", Snackbar.LENGTH_LONG).show();
                                    return;
                                }
                                mainHandler.post(() -> {
                                    obtenerPosts();
                                    Snackbar.make(view, "Post reenviado", Snackbar.LENGTH_LONG).show();
                                });
                            })
                    );
                } catch (Exception e) {
                    Snackbar.make(view, "Error inesperado al reenviar", Snackbar.LENGTH_LONG).show();
                }
            });
        }

        @Override
        public int getItemCount() {
            return lista == null ? 0 : lista.getDocuments().size();
        }

        public void establecerLista(DocumentList<Map<String, Object>> lista) {
            this.lista = lista;
            notifyDataSetChanged();
        }
    }

    void obtenerPosts() {
        Databases databases = new Databases(client);
        Handler mainHandler = new Handler(Looper.getMainLooper());

        try {
            databases.listDocuments(
                    getString(R.string.APPWRITE_DATABASE_ID),
                    getString(R.string.APPWRITE_POSTS_COLLECTION_ID),
                    Arrays.asList(
                            Query.Companion.orderDesc("time"),
                            Query.Companion.limit(58)
                    ),
                    new CoroutineCallback<>((result, error) -> {
                        if (error != null) {
                            Snackbar.make(requireView(), "Error al obtener los posts", Snackbar.LENGTH_LONG).show();
                            return;
                        }
                        mainHandler.post(() -> adapter.establecerLista(result));
                    })
            );
        } catch (AppwriteException e) {
            throw new RuntimeException(e);
        }
    }
}

