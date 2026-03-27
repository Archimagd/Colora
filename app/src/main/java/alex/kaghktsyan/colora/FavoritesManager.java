package alex.kaghktsyan.colora;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

public class FavoritesManager {
    private static final String PREF_NAME = "favorites_prefs";
    private static final String KEY_FAVORITES = "favorite_paths";
    private final SharedPreferences prefs;

    public FavoritesManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void toggleFavorite(String path) {
        Set<String> favorites = getFavorites();
        if (favorites.contains(path)) {
            favorites.remove(path);
        } else {
            favorites.add(path);
        }
        prefs.edit().putStringSet(KEY_FAVORITES, favorites).apply();
    }

    public boolean isFavorite(String path) {
        return getFavorites().contains(path);
    }

    public Set<String> getFavorites() {
        return new HashSet<>(prefs.getStringSet(KEY_FAVORITES, new HashSet<>()));
    }
}
