package com.example.newDemo;

import static org.hamcrest.Matchers.greaterThan;
import com.example.newDemo.entity.Student;
import com.example.newDemo.repository.StudentRepository;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class NewDemoApplicationTests {

	@Container
	@ServiceConnection
	private static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>(
			DockerImageName.parse("artifactory.global.standardchartered.com/postgres:15-alpine")

					.asCompatibleSubstituteFor("postgres")
	).withStartupTimeout(Duration.of(200, ChronoUnit.SECONDS));
//  private static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("artifactory.global.standardchartered.com/postgres:15-alpine");

	@Autowired
	private StudentRepository studentRepository;

	@Autowired
	private MockMvc mockMvc;

	@BeforeEach
	public void resetDatabase(){
		System.setProperty("testcontainers.ryuk.disabled", "true");
		studentRepository.deleteAll();
		studentRepository.save(new Student(1L, "Damini", "Kumari", 50000));
		studentRepository.save(new Student(2L, "John", "Cena", 4000));
		studentRepository.save(new Student(3L, "Taylor", "Kumari", 6000));
		studentRepository.save(new Student(4L, "Anay", "Mishra", 4500));
	}

//	@BeforeEach
//	public void setUp() {
//		studentRepository.deleteAll();
//	}

	// Test for no of rows in db
	// given/when/then format
	@Test
	public void givenStudents_whenGetAllStudents_thenListOfStudents() throws Exception {

		System.out.println(postgreSQLContainer.getDatabaseName());
		System.out.println(postgreSQLContainer.getPassword());
		System.out.println(postgreSQLContainer.getUsername());
		System.out.println(postgreSQLContainer.getJdbcUrl());
		// given - setup or precondition

		// when - action
		ResultActions response = mockMvc.perform(MockMvcRequestBuilders.get("/api/students"));

		// then - verify the output
		response.andExpect(status().isOk());
		response.andExpect(MockMvcResultMatchers.jsonPath("$.length()", CoreMatchers.is(4)));
	}

	@Test
	public void givenStudent_whenAddStudent_thenStudentAdded() throws Exception {
		// create a new student
		Student newStudent = new Student(10L, "New", "Student", 1500);
		String newStudentJson = new ObjectMapper().writeValueAsString(newStudent);

		// Perform the POST request
		ResultActions response = mockMvc.perform(MockMvcRequestBuilders.post("/api/students")
				.contentType(MediaType.APPLICATION_JSON)
				.content(newStudentJson));

		// Verify the response
		response.andExpect(status().isCreated());
		response.andExpect(MockMvcResultMatchers.jsonPath("$.firstName", CoreMatchers.is("New")))
				.andExpect(MockMvcResultMatchers.jsonPath("$.lastName", CoreMatchers.is("Student")))
				.andExpect(MockMvcResultMatchers.jsonPath("$.stipend", CoreMatchers.is(1500)));

		// Verify the number of students increased by 1
		ResultActions getResponse = mockMvc.perform(MockMvcRequestBuilders.get("/api/students"));
		getResponse.andExpect(status().isOk());
		getResponse.andExpect(MockMvcResultMatchers.jsonPath("$.length()", CoreMatchers.is(5)));
	}

	// Test for updating an existing student
	@Test
	public void givenExistingStudent_whenUpdateStudent_thenStudentUpdated() throws Exception {
		// Fetch the student by first name (e.g., 'Ankit')
		ResultActions fetchResponse = mockMvc.perform(MockMvcRequestBuilders.get("/api/students/firstname/John"))
				.andExpect(status().isOk());

		// Extract student ID from the response
		String jsonResponse = fetchResponse.andReturn().getResponse().getContentAsString();
		Long studentId = new ObjectMapper().readTree(jsonResponse).get("id").asLong();

		// Update the student's details
		String updatedStudentJson = "{ \"firstName\": \"Jane\", \"lastName\": \"Doe\", \"stipend\": 2000 }";
		ResultActions response = mockMvc.perform(MockMvcRequestBuilders.put("/api/students/" + studentId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(updatedStudentJson));

		// Verify the response
		response.andExpect(status().isOk());

		// Verify the student is updated
		ResultActions getResponse = mockMvc.perform(MockMvcRequestBuilders.get("/api/students/" + studentId));
		getResponse.andExpect(status().isOk())
				.andExpect(MockMvcResultMatchers.jsonPath("$.firstName", CoreMatchers.is("Jane")))
				.andExpect(MockMvcResultMatchers.jsonPath("$.lastName", CoreMatchers.is("Doe")))
				.andExpect(MockMvcResultMatchers.jsonPath("$.stipend", CoreMatchers.is(2000)));
	}

	// Test for Deleting a student
	@Test
	public void givenStudentId_whenDeleteStudent_thenStudentDeleted() throws Exception {
		// Fetch the student by first name (e.g., 'Ankit')
		ResultActions fetchResponse = mockMvc.perform(MockMvcRequestBuilders.get("/api/students/firstname/Anay"))
				.andExpect(status().isOk());

		// Extract student ID from the response
		String jsonResponse = fetchResponse.andReturn().getResponse().getContentAsString();
		Long studentId = new ObjectMapper().readTree(jsonResponse).get("id").asLong();

		// Perform the DELETE request
		ResultActions response = mockMvc.perform(MockMvcRequestBuilders.delete("/api/students/" + studentId));

		// Verify the response
		response.andExpect(status().isNoContent());

		// Verify the student is deleted
		ResultActions getResponse = mockMvc.perform(MockMvcRequestBuilders.get("/api/students/" + studentId));
		getResponse.andExpect(status().isNotFound());
	}

	// Test for Getting a Single Student by ID
	@Test
	public void givenStudentId_whenGetStudentById_thenStudentReturned() throws Exception {
		// Fetch the student by first name (e.g., 'Damini')
		ResultActions fetchResponse = mockMvc.perform(MockMvcRequestBuilders.get("/api/students/firstname/Damini"))
				.andExpect(status().isOk());

		// Extract student ID from the response
		String jsonResponse = fetchResponse.andReturn().getResponse().getContentAsString();
		Long studentId = new ObjectMapper().readTree(jsonResponse).get("id").asLong();

		// Perform the GET request
		ResultActions response = mockMvc.perform(MockMvcRequestBuilders.get("/api/students/" + studentId));

		// Verify the response
		response.andExpect(status().isOk());
		response.andExpect(MockMvcResultMatchers.jsonPath("$.id", CoreMatchers.is(studentId.intValue())))
				.andExpect(MockMvcResultMatchers.jsonPath("$.firstName", CoreMatchers.is("Damini")));
	}

	// Test for Handling Non-Existent Student
	@Test
	public void givenNonExistingStudentId_whenGetStudentById_thenNotFound() throws Exception {
		// Perform the GET request with a non-existing student ID
		Long nonExistingId = 999L;
		ResultActions response = mockMvc.perform(MockMvcRequestBuilders.get("/api/students/" + nonExistingId));

		// Verify the response
		response.andExpect(status().isNotFound());
	}


	//	Test for stipend above 25000
	@Test
	public void givenStipendAbove20000_whenGetStudents_thenListOfStudents() throws Exception {
		// Perform the GET request
		ResultActions response = mockMvc.perform(MockMvcRequestBuilders.get("/api/students?stipendAbove=20000"));

		// Verify the response
		response.andExpect(status().isOk());
		response.andExpect(MockMvcResultMatchers.jsonPath("$.length()", greaterThan(0)));
	}

}
