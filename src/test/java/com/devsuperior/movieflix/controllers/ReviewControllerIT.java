package com.devsuperior.movieflix.controllers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import com.devsuperior.movieflix.dto.ReviewDTO;
import com.devsuperior.movieflix.tests.TokenUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class ReviewControllerIT {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private TokenUtil tokenUtil;

	@Autowired
	private ObjectMapper objectMapper;

	private String visitorUsername;
	private String visitorPassword;
	private String memberUsername;
	private String memberPassword;
	private String adminUsername;
	private String adminPassword;
	private String memberUsername2;
	private String memberPassword2;
	
	@BeforeEach
	void setUp() throws Exception {
		
		visitorUsername = "bob@gmail.com";
		visitorPassword = "123456";
		memberUsername = "ana@gmail.com";
		memberPassword = "123456";
		adminUsername = "john@gmail.com";
		adminPassword = "123456";
		memberUsername2 = "maria@gmail.com";
		memberPassword2 = "123456";
	}

	@Test
	public void insertShouldReturnUnauthorizedWhenNotValidToken() throws Exception {

		ReviewDTO reviewDTO = new ReviewDTO();
		reviewDTO.setText("Nice Movie!");
		reviewDTO.setMovieId(1L);

		String jsonBody = objectMapper.writeValueAsString(reviewDTO);
		
		ResultActions result =
				mockMvc.perform(post("/reviews")
						.content(jsonBody)
						.contentType(MediaType.APPLICATION_JSON)
						.accept(MediaType.APPLICATION_JSON));

		result.andExpect(status().isUnauthorized());
	}
	
	@Test
	public void insertShouldReturnForbiddenWhenVisitorAuthenticated() throws Exception {
	
		String accessToken = tokenUtil.obtainAccessToken(mockMvc, visitorUsername, visitorPassword);
		
		ReviewDTO reviewDTO = new ReviewDTO();
		reviewDTO.setText("Nice Movie!");
		reviewDTO.setMovieId(1L);

		String jsonBody = objectMapper.writeValueAsString(reviewDTO);
		
		ResultActions result =
				mockMvc.perform(post("/reviews")
						.header("Authorization", "Bearer " + accessToken)
						.content(jsonBody)
						.contentType(MediaType.APPLICATION_JSON)
						.accept(MediaType.APPLICATION_JSON));

		result.andExpect(status().isForbidden());
	}

	@Test
	public void insertShouldReturnForbiddenWhenAdminAuthenticated() throws Exception {

		String accessToken = tokenUtil.obtainAccessToken(mockMvc, adminUsername, adminPassword);

		ReviewDTO reviewDTO = new ReviewDTO();
		reviewDTO.setText("Nice Movie!");
		reviewDTO.setMovieId(1L);

		String jsonBody = objectMapper.writeValueAsString(reviewDTO);

		ResultActions result =
				mockMvc.perform(post("/reviews")
						.header("Authorization", "Bearer " + accessToken)
						.content(jsonBody)
						.contentType(MediaType.APPLICATION_JSON)
						.accept(MediaType.APPLICATION_JSON));

		result.andExpect(status().isForbidden());
	}
	
	@Test
	public void insertShouldInsertReviewWhenMemberAuthenticatedAndValidData() throws Exception {
		
		String accessToken = tokenUtil.obtainAccessToken(mockMvc, memberUsername, memberPassword);
		
		String reviewText = "Nice Movie!";
		long movieId = 1L;
		
		ReviewDTO reviewDTO = new ReviewDTO();
		reviewDTO.setText(reviewText);
		reviewDTO.setMovieId(movieId);

		String jsonBody = objectMapper.writeValueAsString(reviewDTO);
		
		ResultActions result =
				mockMvc.perform(post("/reviews")
						.header("Authorization", "Bearer " + accessToken)
						.content(jsonBody)
						.contentType(MediaType.APPLICATION_JSON)
						.accept(MediaType.APPLICATION_JSON));
		
		result.andExpect(status().isCreated());
		
		result.andExpect(jsonPath("$.id").isNotEmpty());
		result.andExpect(jsonPath("$.text").value(reviewText));
		result.andExpect(jsonPath("$.movieId").value(movieId));
		
		result.andExpect(jsonPath("$.user").isNotEmpty());
		result.andExpect(jsonPath("$.user.id").isNotEmpty());
		result.andExpect(jsonPath("$.user.name").isNotEmpty());
		result.andExpect(jsonPath("$.user.email").value(memberUsername));
	}

	@Test
	public void insertShouldReturnUnproccessableEntityWhenMemberAuthenticatedAndInvalidData() throws Exception {
		
		String accessToken = tokenUtil.obtainAccessToken(mockMvc, memberUsername, memberPassword);
		
		ReviewDTO reviewDTO = new ReviewDTO();
		reviewDTO.setText("        ");
		reviewDTO.setMovieId(1L);

		String jsonBody = objectMapper.writeValueAsString(reviewDTO);

		ResultActions result =
				mockMvc.perform(post("/reviews")
						.header("Authorization", "Bearer " + accessToken)
						.content(jsonBody)
						.contentType(MediaType.APPLICATION_JSON)
						.accept(MediaType.APPLICATION_JSON));

		result.andExpect(status().isUnprocessableEntity());
	}

	@Test
	public void updateShouldUpdateReviewWhenMemberAuthenticatedAndUpdateOwnReview() throws Exception {
		String accessToken = tokenUtil.obtainAccessToken(mockMvc, memberUsername, memberPassword);

		long existingReviewId = 1L;

		ReviewDTO dto = new ReviewDTO();
		dto.setText("Actually is not such a nice movie");
		String jsonBody = objectMapper.writeValueAsString(dto);

		ResultActions result =
				mockMvc.perform(put("/reviews/{id}", existingReviewId)
						.header("Authorization", "Bearer " + accessToken)
						.content(jsonBody)
						.contentType(MediaType.APPLICATION_JSON)
						.accept(MediaType.APPLICATION_JSON));

		result.andExpect(status().isOk());
		result.andExpect(jsonPath("$.id").exists());
		result.andExpect(jsonPath("$.id").value(1L));
		result.andExpect(jsonPath("$.text").value("Actually is not such a nice movie"));
	}

	@Test
	public void updateShouldReturnForbiddenWhenMemberAuthenticatedAndUpdateOthersReview() throws Exception {
		String accessToken = tokenUtil.obtainAccessToken(mockMvc, memberUsername2, memberPassword2);

		long existingReviewId = 1L;

		ReviewDTO dto = new ReviewDTO();
		dto.setText("Actually is not such a nice movie");
		String jsonBody = objectMapper.writeValueAsString(dto);

		ResultActions result =
				mockMvc.perform(put("/reviews/{id}", existingReviewId)
						.header("Authorization", "Bearer " + accessToken)
						.content(jsonBody)
						.contentType(MediaType.APPLICATION_JSON)
						.accept(MediaType.APPLICATION_JSON));

		result.andExpect(status().isForbidden());
	}

	@Test
	public void deleteShouldDeleteAnyReviewWhenAdminAuthenticated() throws Exception {
		String accessToken = tokenUtil.obtainAccessToken(mockMvc, adminUsername, adminPassword);

		long existingReviewId = 1L;

		ResultActions result =
				mockMvc.perform(delete("/reviews/{id}", existingReviewId)
						.header("Authorization", "Bearer " + accessToken)
						.contentType(MediaType.APPLICATION_JSON)
						.accept(MediaType.APPLICATION_JSON));

		result.andExpect(status().isNoContent());
	}
}
