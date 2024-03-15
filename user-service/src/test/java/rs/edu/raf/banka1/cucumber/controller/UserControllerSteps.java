package rs.edu.raf.banka1.cucumber.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.Data;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import rs.edu.raf.banka1.mapper.PermissionMapper;
import rs.edu.raf.banka1.mapper.UserMapper;
import rs.edu.raf.banka1.model.User;
import rs.edu.raf.banka1.repositories.PermissionRepository;
import rs.edu.raf.banka1.repositories.UserRepository;
import rs.edu.raf.banka1.requests.ActivateAccountRequest;
import rs.edu.raf.banka1.requests.CreateUserRequest;
import rs.edu.raf.banka1.requests.EditUserRequest;
import rs.edu.raf.banka1.requests.LoginRequest;
import rs.edu.raf.banka1.responses.*;
import rs.edu.raf.banka1.services.EmailService;
import rs.edu.raf.banka1.services.UserServiceImpl;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UserControllerSteps {
    @LocalServerPort
    private String port;

    private String jwt = "";

    private UserResponse lastReadUserResponse;
    private List<UserResponse> lastReadAllUsersResponse;
    private CreateUserResponse lastCreateUserResponse;
    private EditUserResponse lastEditUserResponse;
    private ActivateAccountResponse lastActivateAccountResponse;
    private User activatedUser;
    private EditUserRequest editUserRequest = new EditUserRequest();
    private CreateUserRequest createUserRequest = new CreateUserRequest();
    private String email;
    private UserMapper userMapper = new UserMapper(new PermissionMapper());

    private UserRepository userRepository;
    private List<UserResponse> userResponses = new ArrayList<>();
    private final String url = "http://localhost:";
    private Long lastid;
    private String password;

    @Data
    class SearchFilter{
        private String email;
        private String firstName;
        private String lastName;
        private String position;
    }

    private SearchFilter searchFilter = new SearchFilter();

    @MockBean
    private EmailService emailService;

    @Given("i am logged in with email {string} and password {string}")
    public void iAmLoggedIn(String email, String password) {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(email);
        loginRequest.setPassword(password);

        HttpEntity<LoginRequest> entity = new HttpEntity<>(loginRequest);
        ResponseEntity<LoginResponse> responseEntity = new RestTemplate().postForEntity(url + port + "/auth/login", entity, LoginResponse.class);
        jwt = responseEntity.getBody().getJwt();

        User user = userRepository.findByEmail(email).get();
        editUserRequest = userMapper.userToEditUserRequest(user);
    }

    @Given("user with email {string} exists")
    public void userWithEmailExists(String email) {
        User user = new User();
        user.setEmail(email);
        user.setPassword("testpassword");
        user.setActivationToken(null);
        userRepository.save(user);
    }

    public UserControllerSteps(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Given("i have email {string}")
    public void iHaveEmail(String email) {
        this.email = email;
    }

    @Given("i have firstName {string}")
    public void iHaveFirstName(String firstName) {
        createUserRequest.setFirstName(firstName);
    }

    @Given("i have lastName {string}")
    public void iHaveLastName(String lastName) {
        createUserRequest.setLastName(lastName);
    }

    @Given("i have jmbg {string}")
    public void iHaveJmbg(String jmbg) {
        createUserRequest.setJmbg(jmbg);
    }

    @Given("i have phone number {string}")
    public void iHavePhoneNumber(String phoneNumber) {
        createUserRequest.setPhoneNumber(phoneNumber);
    }
    @Given("i have position {string}")
    public void iHavePosition(String position) {
        createUserRequest.setPosition(position);
    }
    @Given("i am active")
    public void iAmActive() {
        createUserRequest.setActive(true);
    }

    @Given("I am a user that wants to set password to {string}")
    public void iAmAUserThatWantsToSetPasswordTo(String password) {
        this.password = password;
        User user = new User();
        user.setActivationToken("testtoken");
        user.setEmail("testemail");
        user.setPassword("testpassword");
        userRepository.save(user);
    }

    private String getBody(String path){
        java.net.http.HttpRequest httpRequest = java.net.http.HttpRequest.newBuilder()
                .uri(URI.create(path))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + jwt)
                .method("GET", java.net.http.HttpRequest.BodyPublishers.noBody())
                .build();

        try {
            HttpResponse<String> httpResponse = HttpClient.newHttpClient().send(httpRequest, HttpResponse.BodyHandlers.ofString());
            return httpResponse.body();
        }
        catch (Exception e){
            e.printStackTrace();
            fail("Http GET request error");
            return "";
        }
    }

    private String post(String path, Object objectToPost){

        try {
            if(!jwt.equals("")){
                java.net.http.HttpRequest httpRequest = java.net.http.HttpRequest.newBuilder()
                        .uri(URI.create(path))
                       // .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + jwt)
                        .method("POST", java.net.http.HttpRequest.BodyPublishers.ofString(new ObjectMapper().writeValueAsString(objectToPost)))
                        .build();
                HttpResponse<String> httpResponse = HttpClient.newHttpClient().send(httpRequest, HttpResponse.BodyHandlers.ofString());
                return httpResponse.body();
            }
            else{
                java.net.http.HttpRequest httpRequest = java.net.http.HttpRequest.newBuilder()
                        .uri(URI.create(path))
                        .method("POST", java.net.http.HttpRequest.BodyPublishers.ofString(new ObjectMapper().writeValueAsString(objectToPost)))
                        .build();
                HttpResponse<String> httpResponse = HttpClient.newHttpClient().send(httpRequest, HttpResponse.BodyHandlers.ofString());
                return httpResponse.body();
            }
        }
        catch (Exception e){
            e.printStackTrace();
            fail("Http GET request error");
            return "";
        }
    }

//    private String post(String path, Object objectToPost) throws JsonProcessingException {
//        RestTemplate restTemplate = new RestTemplate();
//
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
//        headers.setBearerAuth(jwt);
//        HttpEntity<Object> request = new HttpEntity<>(objectToPost, headers);
//
//        ResponseEntity<String> response = restTemplate.exchange(path, org.springframework.http.HttpMethod.POST, request, String.class);
//        return response.getBody();
//
//    }

    //private String put(String path, Object objectToPut){
    //    try {
    //        java.net.http.HttpRequest httpRequest = java.net.http.HttpRequest.newBuilder()
    //                .uri(URI.create(path))
    //                // .header("Content-Type", "application/json")
    //                .header("Authorization", "Bearer " + jwt)
    //                .method("PUT", java.net.http.HttpRequest.BodyPublishers.ofString(new ObjectMapper().writeValueAsString(objectToPut)))
    //                .build();
    //        HttpResponse<String> httpResponse = HttpClient.newHttpClient().send(httpRequest, HttpResponse.BodyHandlers.ofString());
    //        return httpResponse.body();
    //    }
    //    catch (Exception e){
    //        e.printStackTrace();
    //        fail("Http GET request error");
    //        return "";
    //    }
    //}

    private String put(String path, Object objectToPut) throws JsonProcessingException {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        headers.setBearerAuth(jwt);
        HttpEntity<Object> request = new HttpEntity<>(objectToPut, headers);

        ResponseEntity<String> response = restTemplate.exchange(path, org.springframework.http.HttpMethod.PUT, request, String.class);
        return response.getBody();
    }


    @When("User calls get on {string}")
    public void iSendAGETRequestTo(String path) {
        userResponses.clear();
        ObjectMapper objectMapper = new ObjectMapper();
        if(path.equals("/user/getAll")) {
            try {
                lastReadAllUsersResponse = objectMapper.readValue(getBody(url + port + path), new TypeReference<List<UserResponse>>() {});
            } catch (Exception e) {
                e.printStackTrace();
                fail("Failed to parse response body");
            }
            userRepository.findAll().forEach(user -> userResponses.add(userMapper.userToUserResponse(user)));
        }
        else if(path.startsWith("/user/get/")) {
            try {
                lastReadUserResponse = objectMapper.readValue(getBody(url + port + path), UserResponse.class);
            }
            catch (Exception e){
                e.printStackTrace();
                fail("Failed to parse response body");
            }
            String[] split = path.split("/");
            email = split[split.length - 1];
        }
        else if(path.equals("/user/search")) {
            try {
                lastReadAllUsersResponse = objectMapper.readValue(getBody(url + port + path), new TypeReference<List<UserResponse>>() {});
            }
            catch (Exception e){
                e.printStackTrace();
                fail("Failed to parse response body");
            }
            userRepository.findAll().forEach(user -> {
                if(searchFilter.getEmail() != null && !user.getEmail().equals(searchFilter.getEmail())) return;
                if(searchFilter.getFirstName() != null && !user.getFirstName().equalsIgnoreCase(searchFilter.getFirstName())) return;
                if(searchFilter.getLastName() != null && !user.getLastName().equalsIgnoreCase(searchFilter.getLastName())) return;
                if(searchFilter.getPosition() != null && !user.getPosition().equalsIgnoreCase(searchFilter.getPosition())) return;
                userResponses.add(userMapper.userToUserResponse(user));
            });
        }
        else if(path.startsWith("/user/")) {
            try {
                lastReadUserResponse = objectMapper.readValue(getBody(url + port + path), UserResponse.class);
            }
            catch (Exception e){
                e.printStackTrace();
                fail("Failed to parse response body");
            }
            String[] split = path.split("/");
            lastid = Long.parseLong(split[split.length - 1]);

        }
    }

   @When("user calls POST on {string}")
   public void userCallsPostOn(String path) {
        ObjectMapper objectMapper = new ObjectMapper();
       if(path.equals("/user/createUser")) {
           try{
               String tmp = post(url + port + path, createUserRequest);
               lastCreateUserResponse = objectMapper.readValue(tmp, CreateUserResponse.class);
           }
           catch (Exception e){
               e.printStackTrace();
               fail("Failed to parse response body");
           }
       }
   }

   @When("I go to {string}")
    public void iGoTo(String path) {
        activatedUser = userRepository.findByActivationToken("testtoken").get();
       ActivateAccountRequest activateAccountRequest = new ActivateAccountRequest();
       activateAccountRequest.setPassword(password);
       ObjectMapper objectMapper = new ObjectMapper();
       try {
           lastActivateAccountResponse = objectMapper.readValue(post(url + port + path, activateAccountRequest), ActivateAccountResponse.class);
       } catch (Exception e) {
           e.printStackTrace();
           fail("Failed to parse response body");
       }
   }

   @When("i select user with email {string} to change")
   public void whenISelectUserWithEmailToChange(String email) {
       editUserRequest.setEmail(email);
   }

   @When("i change first name to {string}")
    public void whenIChangeFirstNameTo(String firstName) {
         editUserRequest.setFirstName(firstName);
    }

    //TODO: treba put da bude
    @When("i send PUT request to {string}")
    public void whenISendPUTRequestTo(String path) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            lastEditUserResponse = objectMapper.readValue(put(url + port + path, editUserRequest), EditUserResponse.class);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Failed to parse response body");
        }
    }

    @Then("i should get my id as a response")
    public void iShouldGetMyIdAsAResponse() {
        assertThat(lastCreateUserResponse.getUserId()).isNotNull();
    }

    @Then("email should be sent to me")
    public void emailShouldBeSentToMe() {
        verify(emailService).sendActivationEmail(eq(createUserRequest.getEmail()), anyString(), anyString());
    }

    @Given("user provides email {string}")
    public void userProvidesEmail(String email) {
        searchFilter.setEmail(email);
    }

    @Given("user provides first name {string}")
    public void userProvidesFirstName(String firstName) {
        searchFilter.setFirstName(firstName);
    }

    @Given("user provides last name {string}")
    public void userProvidesLastName(String lastName) {
        searchFilter.setLastName(lastName);
    }

    @Given("user provides position {string}")
    public void userProvidesPosition(String position) {
        searchFilter.setPosition(position);
    }

//    @Then("Response status is {string}")
//    public void theResponseStatusShouldBe(String code) {

//        assertThat(lastReadAllUsersResponse.getStatusCode().toString()).isEqualTo(code);
//    }
//
    @Then("Response body is the correct JSON list of users")
    public void theResponseBodyShouldBeAListOfUsers() {
        assertThat(lastReadAllUsersResponse).hasSameElementsAs(userResponses);
    }

    @Then("Response body is the correct user JSON")
    public void responseBodyIsTheCorrectUserJSON() {
        //TODO: throw appropriate exception?
        if(email!=null) {
            UserResponse userResponse = userMapper.userToUserResponse(userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found")));
            assertThat(lastReadUserResponse).isEqualTo(userResponse);
        }
        else {
            UserResponse userResponse = userMapper.userToUserResponse(userRepository.findById(lastid).get());
            assertThat(lastReadUserResponse).isEqualTo(userResponse);
        }
    }

    @Then("I should have my password set to {string}")
    public void iShouldHaveMyPasswordSetTo(String password) {
        activatedUser = userRepository.findById(activatedUser.getUserId()).get();
        assertThat(activatedUser.getPassword()).isEqualTo(password);
    }

    @Then("user with email {string} has his first name changed to {string}")
    public void userWithEmailHasHisFirstNameChangedTo(String email, String firstName) {
        User user = userRepository.findByEmail(email).get();
        assertThat(user.getFirstName()).isEqualTo(firstName);
    }
}
