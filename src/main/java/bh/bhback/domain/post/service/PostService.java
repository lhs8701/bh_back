package bh.bhback.domain.post.service;

import bh.bhback.domain.image.dto.ImageDto;
import bh.bhback.domain.image.service.ImageService;
import bh.bhback.domain.place.dto.CurPlaceDto;
import bh.bhback.domain.place.entity.Place;
import bh.bhback.domain.place.repository.PlaceJpaRepository;
import bh.bhback.domain.place.service.PlaceService;
import bh.bhback.domain.post.dto.FeedResponseDto;
import bh.bhback.domain.post.dto.PostRequestDto;
import bh.bhback.domain.post.dto.PostResponseDto;
import bh.bhback.domain.post.dto.PostUpdateParam;
import bh.bhback.domain.post.entity.Post;
import bh.bhback.domain.post.repository.PostJpaRepository;
import bh.bhback.domain.user.entity.User;
import bh.bhback.domain.user.repository.UserJpaRepository;
import bh.bhback.global.error.advice.exception.CAccessDeniedException;
import bh.bhback.global.error.advice.exception.CUserNotFoundException;
import bh.bhback.global.error.advice.exception.CPostNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
@Slf4j
public class PostService {

    private final PostJpaRepository postJpaRepository;
    private final UserJpaRepository userJpaRepository;
    private final PlaceJpaRepository placeJpaRepository;

    private final ImageService imageService;
    private final PlaceService placeService;

    @Transactional
    public Long create(PostRequestDto postRequestDto, MultipartFile file, User user) {
        Long contentId = postRequestDto.getPlace().getContentId();
        Optional<Place> placeOptional = placeJpaRepository.findByContentId(contentId);
        Place place;

        if (placeOptional.isEmpty())
            place = placeJpaRepository.save(postRequestDto.getPlace());
        else
            place = placeOptional.get();

        ImageDto imageDto = imageService.uploadPostImage(file);
        Post post = postRequestDto.toEntity(user, imageDto.toEntity(), place);
        return postJpaRepository.save(post).getId();
    }

    @Transactional
    public Long update(Long postId, PostUpdateParam postUpdateParam, User user) {
        Post post = postJpaRepository.findById(postId)
                .orElseThrow(CPostNotFoundException::new);
        if (!post.getUser().getUserId().equals(user.getUserId())){
            throw new CAccessDeniedException();
        }
        post.setTag(postUpdateParam.getTag());
        post.setPlace(postUpdateParam.getPlace());
        return postId;
    }

    @Transactional
    public void delete(Long postId, User user) {
        Post post = postJpaRepository.findById(postId)
                .orElseThrow(CPostNotFoundException::new);
        if (!post.getUser().getUserId().equals(user.getUserId())){
            throw new CAccessDeniedException();
        }
        postJpaRepository.deleteById(postId);
    }

    @Transactional
    public PostResponseDto getPost(Long postId) {
        Post post = postJpaRepository.findById(postId)
                .orElseThrow(CPostNotFoundException::new);
        return new PostResponseDto(post);
    }

    @Transactional // 최신순 정렬(임시)
    public List<FeedResponseDto> getFeed(Pageable pageable) {
        List<Post> postList = postJpaRepository.findAllByOrderByCreatedDateDesc(pageable)
                .orElseThrow(CPostNotFoundException::new);
        return postList.stream()
                .map(FeedResponseDto::new)
                .collect(Collectors.toList());
    }

    /**
     * @param pageable
     * @param curPlaceDto (현재 위치)
     * @return 현재 부터 떨어진 거리순으로 정렬된 Feed
     */
    @Transactional
    public List<FeedResponseDto> getFeedOrderByDistance(Pageable pageable, CurPlaceDto curPlaceDto) {
        List<Post> postList = postJpaRepository.findAllByOrderByCreatedDateDesc(pageable)
                .orElseThrow(CPostNotFoundException::new);
        List<FeedResponseDto> feedList = new ArrayList<FeedResponseDto>();

        double curX = curPlaceDto.getCurX();
        double curY = curPlaceDto.getCurY();

        for(Post post:postList){
            //상대 거리 구하기
            double MapX = post.getPlace().getMapX();
            double MapY = post.getPlace().getMapY();
            double distance = placeService.getDistance(curX, curY, MapX, MapY);
            feedList.add(new FeedResponseDto(post));
        }

        //정렬 알고리즘 구현
        Comparator<FeedResponseDto> comparator = new Comparator<FeedResponseDto>() {
            @Override
            public int compare(FeedResponseDto f1, FeedResponseDto f2) {
                double distance = (f1.getDistance()-f2.getDistance());

                return (int)Math.round(distance);
            }
        };

        Collections.sort(feedList, comparator);

        return feedList;
    }

    @Transactional // 최신순 정렬(임시)
    public List<FeedResponseDto> getUserFeed(Long userId, Pageable pageable) {
        User user = userJpaRepository.findById(userId).orElseThrow(CUserNotFoundException::new);
        List<Post> postList = postJpaRepository.findAllByUserOrderByCreatedDateDesc(user, pageable)
                .orElseThrow(CPostNotFoundException::new);
        return postList.stream()
                .map(FeedResponseDto::new)
                .collect(Collectors.toList());
    }

    @Transactional // 최신순 정렬(임시)
    public List<FeedResponseDto> getUserFeed(User user, Pageable pageable) {
        userJpaRepository.findById(user.getUserId()).orElseThrow(CUserNotFoundException::new);
        List<Post> postList = postJpaRepository.findAllByUserOrderByCreatedDateDesc(user, pageable)
                .orElseThrow(CPostNotFoundException::new);
        return postList.stream()
                .map(FeedResponseDto::new)
                .collect(Collectors.toList());
    }
}
