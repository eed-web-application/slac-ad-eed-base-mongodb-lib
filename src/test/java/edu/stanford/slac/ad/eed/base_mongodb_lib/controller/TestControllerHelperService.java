package edu.stanford.slac.ad.eed.base_mongodb_lib.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.stanford.slac.ad.eed.baselib.api.v1.dto.*;
import edu.stanford.slac.ad.eed.baselib.auth.JWTHelper;
import edu.stanford.slac.ad.eed.baselib.config.AppProperties;
import edu.stanford.slac.ad.eed.baselib.exception.ControllerLogicException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;
import java.util.Optional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@Service()
public class TestControllerHelperService {
    private final JWTHelper jwtHelper;
    private final AppProperties appProperties;

    public TestControllerHelperService(JWTHelper jwtHelper, AppProperties appProperties) {
        this.jwtHelper = jwtHelper;
        this.appProperties = appProperties;
    }


    public ApiResultResponse<PersonDTO> getMe(
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            Optional<String> userInfo) throws Exception {
        MockHttpServletRequestBuilder getBuilder =
                get("/v1/auth/me")
                        .accept(MediaType.APPLICATION_JSON);
        userInfo.ifPresent(login -> getBuilder.header(appProperties.getUserHeaderName(), jwtHelper.generateJwt(login)));
        MvcResult result = mockMvc.perform(
                        getBuilder
                )
                .andExpect(resultMatcher)
                .andReturn();
        Optional<ControllerLogicException> someException = Optional.ofNullable((ControllerLogicException) result.getResolvedException());
        if (someException.isPresent()) {
            throw someException.get();
        }
        return new ObjectMapper().readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<>() {
                });
    }

    public ApiResultResponse<List<PersonDTO>> findUsers(
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            Optional<String> userInfo,
            Optional<String> search) throws Exception {
        MockHttpServletRequestBuilder getBuilder =
                get("/v1/auth/users")
                        .accept(MediaType.APPLICATION_JSON);
        userInfo.ifPresent(login -> getBuilder.header(appProperties.getUserHeaderName(), jwtHelper.generateJwt(login)));
        search.ifPresent(string -> getBuilder.param("search", string));
        MvcResult result = mockMvc.perform(
                        getBuilder
                )
                .andExpect(resultMatcher)
                .andReturn();
        Optional<ControllerLogicException> someException = Optional.ofNullable((ControllerLogicException) result.getResolvedException());
        if (someException.isPresent()) {
            throw someException.get();
        }
        return new ObjectMapper().readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<>() {
                });
    }

    public ApiResultResponse<List<GroupDTO>> findGroups(
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            Optional<String> userInfo,
            Optional<String> search) throws Exception {
        MockHttpServletRequestBuilder getBuilder =
                get("/v1/auth/groups")
                        .accept(MediaType.APPLICATION_JSON);
        userInfo.ifPresent(login -> getBuilder.header(appProperties.getUserHeaderName(), jwtHelper.generateJwt(login)));
        search.ifPresent(string -> getBuilder.param("search", string));
        MvcResult result = mockMvc.perform(
                        getBuilder
                )
                .andExpect(resultMatcher)
                .andReturn();
        Optional<ControllerLogicException> someException = Optional.ofNullable((ControllerLogicException) result.getResolvedException());
        if (someException.isPresent()) {
            throw someException.get();
        }
        return new ObjectMapper().readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<>() {
                });
    }

    public ApiResultResponse<AuthenticationTokenDTO> createNewAuthenticationToken(
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            Optional<String> userInfo,
            NewAuthenticationTokenDTO newAuthenticationTokenDTO) throws Exception {
        MockHttpServletRequestBuilder postBuilder =
                post("/v1/auth/application-token")
                        .content(
                                new ObjectMapper().writeValueAsString(
                                        newAuthenticationTokenDTO
                                )
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON);
        userInfo.ifPresent(login -> postBuilder.header(appProperties.getUserHeaderName(), jwtHelper.generateJwt(login)));
        MvcResult result = mockMvc.perform(
                        postBuilder
                )
                .andExpect(resultMatcher)
                .andReturn();
        Optional<ControllerLogicException> someException = Optional.ofNullable((ControllerLogicException) result.getResolvedException());
        if (someException.isPresent()) {
            throw someException.get();
        }
        return new ObjectMapper().readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<>() {
                });
    }

    public ApiResultResponse<List<AuthenticationTokenDTO>> getAllAuthenticationToken(
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            Optional<String> userInfo) throws Exception {
        MockHttpServletRequestBuilder getBuilder =
                get("/v1/auth/application-token")
                        .accept(MediaType.APPLICATION_JSON);
        userInfo.ifPresent(login -> getBuilder.header(appProperties.getUserHeaderName(), jwtHelper.generateJwt(login)));
        MvcResult result = mockMvc.perform(
                        getBuilder
                )
                .andExpect(resultMatcher)
                .andReturn();
        Optional<ControllerLogicException> someException = Optional.ofNullable((ControllerLogicException) result.getResolvedException());
        if (someException.isPresent()) {
            throw someException.get();
        }
        return new ObjectMapper().readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<>() {
                });
    }

    public ApiResultResponse<Boolean> deleteAuthenticationToken(
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            Optional<String> userInfo,
            String id) throws Exception {
        MockHttpServletRequestBuilder deleteBuilder =
                delete("/v1/auth/application-token/{id}", id)
                        .accept(MediaType.APPLICATION_JSON);
        userInfo.ifPresent(login -> deleteBuilder.header(appProperties.getUserHeaderName(), jwtHelper.generateJwt(login)));
        MvcResult result = mockMvc.perform(
                        deleteBuilder
                )
                .andExpect(resultMatcher)
                .andReturn();
        Optional<ControllerLogicException> someException = Optional.ofNullable((ControllerLogicException) result.getResolvedException());
        if (someException.isPresent()) {
            throw someException.get();
        }
        return new ObjectMapper().readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<>() {
                });
    }

    public ApiResultResponse<Boolean> createNewRootUser(
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            Optional<String> userInfo,
            String userEmail) throws Exception {
        MockHttpServletRequestBuilder requestBuilder =
                post("/v1/auth/root/{email}", userEmail)
                        .accept(MediaType.APPLICATION_JSON);
        userInfo.ifPresent(login -> requestBuilder.header(appProperties.getUserHeaderName(), jwtHelper.generateJwt(login)));
        MvcResult result = mockMvc.perform(
                        requestBuilder
                )
                .andExpect(resultMatcher)
                .andReturn();
        Optional<ControllerLogicException> someException = Optional.ofNullable((ControllerLogicException) result.getResolvedException());
        if (someException.isPresent()) {
            throw someException.get();
        }
        return new ObjectMapper().readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<>() {
                });
    }

    public ApiResultResponse<Boolean> deleteRootUser(
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            Optional<String> userInfo,
            String userEmail) throws Exception {
        MockHttpServletRequestBuilder requestBuilder =
                delete("/v1/auth/root/{email}", userEmail)
                        .accept(MediaType.APPLICATION_JSON);
        userInfo.ifPresent(login -> requestBuilder.header(appProperties.getUserHeaderName(), jwtHelper.generateJwt(login)));
        MvcResult result = mockMvc.perform(
                        requestBuilder
                )
                .andExpect(resultMatcher)
                .andReturn();
        Optional<ControllerLogicException> someException = Optional.ofNullable((ControllerLogicException) result.getResolvedException());
        if (someException.isPresent()) {
            throw someException.get();
        }
        return new ObjectMapper().readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<>() {
                });
    }

    public ApiResultResponse<List<AuthorizationDTO>> findAllRootUser(
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            Optional<String> userInfo) throws Exception {
        MockHttpServletRequestBuilder requestBuilder =
                get("/v1/auth/root")
                        .accept(MediaType.APPLICATION_JSON);
        userInfo.ifPresent(login -> requestBuilder.header(appProperties.getUserHeaderName(), jwtHelper.generateJwt(login)));
        MvcResult result = mockMvc.perform(
                        requestBuilder
                )
                .andExpect(resultMatcher)
                .andReturn();
        Optional<ControllerLogicException> someException = Optional.ofNullable((ControllerLogicException) result.getResolvedException());
        if (someException.isPresent()) {
            throw someException.get();
        }
        return new ObjectMapper().readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<>() {
                });
    }
}
