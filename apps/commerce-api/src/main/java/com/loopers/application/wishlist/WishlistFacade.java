package com.loopers.application.wishlist;

import com.loopers.domain.product.ProductService;
import com.loopers.domain.wishlist.WishlistService;
import com.loopers.domain.wishlist.event.ProductLikedEvent;
import com.loopers.domain.wishlist.event.ProductUnlikedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Component
public class WishlistFacade {

    private final WishlistService wishlistService;
    private final ProductService productService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void addLike(Long userId, Long productId) {
        wishlistService.add(userId, productId);
        productService.increaseLikeCount(productId);
        eventPublisher.publishEvent(new ProductLikedEvent(productId));
    }

    @Transactional
    public void removeLike(Long userId, Long productId) {
        wishlistService.remove(userId, productId);
        productService.decreaseLikeCount(productId);
        eventPublisher.publishEvent(new ProductUnlikedEvent(productId));
    }

    public List<WishlistInfo> getLikedProducts(Long userId) {
        return wishlistService.getListWithDetails(userId)
                .stream()
                .map(WishlistInfo::from)
                .toList();
    }
}
