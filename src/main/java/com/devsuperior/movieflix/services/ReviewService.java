package com.devsuperior.movieflix.services;

import com.devsuperior.movieflix.dto.ReviewDTO;
import com.devsuperior.movieflix.entities.Review;
import com.devsuperior.movieflix.entities.User;
import com.devsuperior.movieflix.repositories.MovieRepository;
import com.devsuperior.movieflix.repositories.ReviewRepository;
import com.devsuperior.movieflix.services.exceptions.DatabaseException;
import com.devsuperior.movieflix.services.exceptions.ForbiddenException;
import com.devsuperior.movieflix.services.exceptions.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ReviewService {

	@Autowired
	private ReviewRepository repository;
	
	@Autowired
	private MovieRepository movieRepository;
	
	@Autowired
	private AuthService authService;

	@Transactional(readOnly = true)
	public List<ReviewDTO> findAll() {
		List<Review> list = repository.findAll();

		return list.stream().map(x -> new ReviewDTO(x)).collect(Collectors.toList());
	}

	@Transactional
	public ReviewDTO insert(ReviewDTO dto) {
		User user = authService.authenticated();
		Review entity = new Review();
		entity.setMovie(movieRepository.getOne(dto.getMovieId()));
		entity.setUser(user);
		entity.setText(dto.getText());
		repository.save(entity);
		return new ReviewDTO(entity);
	}

	@Transactional
	public ReviewDTO update(Long id, ReviewDTO dto) {
		try {
			User user = authService.authenticated();
			Review entity = repository.getOne(id);

			if (entity.getUser().getId().equals(user.getId())) {
				entity.setText(dto.getText());
				entity = repository.save(entity);
				return new ReviewDTO(entity);
			} else {
				throw new ForbiddenException("Access denied");
			}
		}
		catch (EntityNotFoundException e) {
			throw new ResourceNotFoundException("Id not found " + id);
		}
	}

	public void delete(Long id) {
		try {
			if (isValidatedToDeleteReview(id)) {
				repository.deleteById(id);
			} else {
				throw new ForbiddenException("Access denied");
			}
		}
		catch (EmptyResultDataAccessException e) {
			throw new ResourceNotFoundException("Id not found " + id);
		}
		catch (DataIntegrityViolationException e) {
			throw new DatabaseException("Integrity violation");
		}
	}

	public Boolean isValidatedToDeleteReview(Long reviewId) {
		User user = authService.authenticated();
		Optional<Review> obj = repository.findById(reviewId);
		Review review = obj.orElseThrow(() -> new ResourceNotFoundException("Id not found " + reviewId));

		if (review.getUser().getId().equals(user.getId()) || user.hasRole("ROLE_ADMIN")) {
			return true;
		}
		return false;
	}
}
