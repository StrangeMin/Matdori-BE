package com.matdori.matdori.service;

import com.matdori.matdori.domain.Store;
import com.matdori.matdori.domain.StoreFavorite;
import com.matdori.matdori.domain.User;
import com.matdori.matdori.repositoy.StoreFavoriteRepository;
import com.matdori.matdori.repositoy.StoreRepository;
import com.matdori.matdori.repositoy.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final StoreFavoriteRepository storeFavoriteRepository;
    private final StoreRepository storeRepository;

    public User findOne(Long id) { return userRepository.findOne(id); }
    public List<Store> findAllFavoriteStore(Long id) { return storeFavoriteRepository.findAllFavoriteStore(id);}

    @Transactional
    public void createFavoriteStore(Long storeId, Long userId) {
        User user = userRepository.findOne(userId);
        Store store = storeRepository.findOne(storeId);
        StoreFavorite storeFavorite = StoreFavorite.createStoreFavorite(user, store);
        storeFavoriteRepository.saveStoreFavorite(storeFavorite);
    }
}
