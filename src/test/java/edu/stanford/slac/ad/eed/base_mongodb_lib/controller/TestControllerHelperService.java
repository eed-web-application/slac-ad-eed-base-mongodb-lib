package edu.stanford.slac.ad.eed.base_mongodb_lib.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.stanford.slac.ad.eed.baselib.api.v1.dto.*;
import edu.stanford.slac.ad.eed.baselib.api.v2.dto.*;
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
    private final ObjectMapper objectMapper;

    public TestControllerHelperService(JWTHelper jwtHelper, AppProperties appProperties, ObjectMapper objectMapper) {
        this.jwtHelper = jwtHelper;
        this.appProperties = appProperties;
        this.objectMapper = objectMapper;
    }

    public ApiResultResponse<PersonDetailsDTO> getMe(
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            Optional<String> userInfo,
            Optional<String> impersonateUserInfo) throws Exception {
        MockHttpServletRequestBuilder requestBuilder =
                get("/v1/auth/me")
                        .accept(MediaType.APPLICATION_JSON);
        userInfo.ifPresent(login -> requestBuilder.header(appProperties.getUserHeaderName(), jwtHelper.generateJwt(login)));
        return executeHttpRequest(
                new TypeReference<>() {
                },
                mockMvc,
                resultMatcher,
                userInfo,
                impersonateUserInfo,
                requestBuilder
        );
    }

    public ApiResultResponse<List<PersonDTO>> findUsers(
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            Optional<String> userInfo,
            Optional<String> impersonateUserInfo,
            Optional<String> search) throws Exception {
        MockHttpServletRequestBuilder requestBuilder =
                get("/v1/auth/users")
                        .accept(MediaType.APPLICATION_JSON);
        userInfo.ifPresent(login -> requestBuilder.header(appProperties.getUserHeaderName(), jwtHelper.generateJwt(login)));
        search.ifPresent(string -> requestBuilder.param("search", string));
        return executeHttpRequest(
                new TypeReference<>() {
                },
                mockMvc,
                resultMatcher,
                userInfo,
                impersonateUserInfo,
                requestBuilder
        );
    }

    public ApiResultResponse<List<GroupDTO>> findGroups(
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            Optional<String> userInfo,
            Optional<String> impersonateUserInfo,
            Optional<String> search) throws Exception {
        MockHttpServletRequestBuilder requestBuilder =
                get("/v1/auth/groups")
                        .accept(MediaType.APPLICATION_JSON);
        userInfo.ifPresent(login -> requestBuilder.header(appProperties.getUserHeaderName(), jwtHelper.generateJwt(login)));
        search.ifPresent(string -> requestBuilder.param("search", string));
        return executeHttpRequest(
                new TypeReference<>() {
                },
                mockMvc,
                resultMatcher,
                userInfo,
                impersonateUserInfo,
                requestBuilder
        );
    }

    public ApiResultResponse<AuthenticationTokenDTO> createNewAuthenticationToken(
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            Optional<String> userInfo,
            Optional<String> impersonateUserInfo,
            NewAuthenticationTokenDTO newAuthenticationTokenDTO) throws Exception {
        MockHttpServletRequestBuilder requestBuilder =
                post("/v1/auth/application-token")
                        .content(
                                new ObjectMapper().writeValueAsString(
                                        newAuthenticationTokenDTO
                                )
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON);
        userInfo.ifPresent(login -> requestBuilder.header(appProperties.getUserHeaderName(), jwtHelper.generateJwt(login)));
        return executeHttpRequest(
                new TypeReference<>() {
                },
                mockMvc,
                resultMatcher,
                userInfo,
                impersonateUserInfo,
                requestBuilder
        );
    }

    public ApiResultResponse<List<AuthenticationTokenDTO>> getAllAuthenticationToken(
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            Optional<String> userInfo,
            Optional<String> impersonateUserInfo) throws Exception {
        MockHttpServletRequestBuilder requestBuilder =
                get("/v1/auth/application-token")
                        .accept(MediaType.APPLICATION_JSON);
        userInfo.ifPresent(login -> requestBuilder.header(appProperties.getUserHeaderName(), jwtHelper.generateJwt(login)));
        return executeHttpRequest(
                new TypeReference<>() {
                },
                mockMvc,
                resultMatcher,
                userInfo,
                impersonateUserInfo,
                requestBuilder
        );
    }

    public ApiResultResponse<Boolean> deleteAuthenticationToken(
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            Optional<String> userInfo,
            Optional<String> impersonateUserInfo,
            String id) throws Exception {
        MockHttpServletRequestBuilder requestBuilder =
                delete("/v1/auth/application-token/{id}", id)
                        .accept(MediaType.APPLICATION_JSON);
        userInfo.ifPresent(login -> requestBuilder.header(appProperties.getUserHeaderName(), jwtHelper.generateJwt(login)));
        return executeHttpRequest(
                new TypeReference<>() {
                },
                mockMvc,
                resultMatcher,
                userInfo,
                impersonateUserInfo,
                requestBuilder
        );
    }

    public ApiResultResponse<Boolean> createNewRootUser(
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            Optional<String> userInfo,
            Optional<String> impersonateUserInfo,
            String userEmail) throws Exception {
        MockHttpServletRequestBuilder requestBuilder =
                post("/v1/auth/root/{email}", userEmail)
                        .accept(MediaType.APPLICATION_JSON);
        userInfo.ifPresent(login -> requestBuilder.header(appProperties.getUserHeaderName(), jwtHelper.generateJwt(login)));
        return executeHttpRequest(
                new TypeReference<>() {
                },
                mockMvc,
                resultMatcher,
                userInfo,
                impersonateUserInfo,
                requestBuilder
        );
    }

    public ApiResultResponse<Boolean> deleteRootUser(
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            Optional<String> userInfo,
            Optional<String> impersonateUserInfo,
            String userEmail) throws Exception {
        MockHttpServletRequestBuilder requestBuilder =
                delete("/v1/auth/root/{email}", userEmail)
                        .accept(MediaType.APPLICATION_JSON);
        userInfo.ifPresent(login -> requestBuilder.header(appProperties.getUserHeaderName(), jwtHelper.generateJwt(login)));
        return executeHttpRequest(
                new TypeReference<>() {
                },
                mockMvc,
                resultMatcher,
                userInfo,
                impersonateUserInfo,
                requestBuilder
        );
    }

    public ApiResultResponse<List<AuthorizationDTO>> findAllRootUser(
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            Optional<String> userInfo,
            Optional<String> impersonateUserInfo) throws Exception {
        MockHttpServletRequestBuilder requestBuilder =
                get("/v1/auth/root")
                        .accept(MediaType.APPLICATION_JSON);
        userInfo.ifPresent(login -> requestBuilder.header(appProperties.getUserHeaderName(), jwtHelper.generateJwt(login)));
        return executeHttpRequest(
                new TypeReference<>() {
                },
                mockMvc,
                resultMatcher,
                userInfo,
                impersonateUserInfo,
                requestBuilder
        );
    }

    /**
     * Create new local group
     *
     * @param mockMvc
     * @param resultMatcher
     * @param userInfo
     * @param newLocalGroupDTO
     * @return
     * @throws Exception
     */
    public ApiResultResponse<String> v2AuthorizationControllerCreateNewLocalGroup(
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            Optional<String> userInfo,
            Optional<String> impersonateUserInfo,
            NewLocalGroupDTO newLocalGroupDTO) throws Exception {
        MockHttpServletRequestBuilder requestBuilder =
                post("/v2/auth/local/group")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                objectMapper.writeValueAsString(
                                        newLocalGroupDTO
                                )
                        );
        userInfo.ifPresent(login -> requestBuilder.header(appProperties.getUserHeaderName(), jwtHelper.generateJwt(login)));
        return executeHttpRequest(
                new TypeReference<>() {
                },
                mockMvc,
                resultMatcher,
                userInfo,
                impersonateUserInfo,
                requestBuilder
        );
    }


    /**
     * Update a local group
     *
     * @param mockMvc
     * @param resultMatcher
     * @param userInfo
     * @param localGroupId
     * @param updateLocalGroupDTO
     * @return
     * @throws Exception
     */
    public ApiResultResponse<Boolean> v2AuthorizationControllerUpdateLocalGroup(
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            Optional<String> userInfo,
            Optional<String> impersonateUserInfo,
            String localGroupId,
            UpdateLocalGroupDTO updateLocalGroupDTO) throws Exception {
        MockHttpServletRequestBuilder requestBuilder =
                put
                        (
                                "/v2/auth/local/group/{localGroupId}",
                                localGroupId
                        )
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                objectMapper.writeValueAsString(
                                        updateLocalGroupDTO
                                )
                        );
        userInfo.ifPresent(login -> requestBuilder.header(appProperties.getUserHeaderName(), jwtHelper.generateJwt(login)));
        return executeHttpRequest(
                new TypeReference<>() {
                },
                mockMvc,
                resultMatcher,
                userInfo,
                impersonateUserInfo,
                requestBuilder
        );
    }

    /**
     * Find local group
     *
     * @param mockMvc
     * @param resultMatcher
     * @param userInfo
     * @param anchorId
     * @param contextSize
     * @param limit
     * @param search
     * @return
     * @throws Exception
     */
    public ApiResultResponse<List<LocalGroupDTO>> v2AuthorizationControllerFindLocalGroup(
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            Optional<String> userInfo,
            Optional<String> impersonateUserInfo,
            Optional<String> anchorId,
            Optional<Integer> contextSize,
            Optional<Integer> limit,
            Optional<String> search) throws Exception {
        MockHttpServletRequestBuilder requestBuilder =
                get
                        (
                                "/v2/auth/local/group"
                        )
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON);
        userInfo.ifPresent(login -> requestBuilder.header(appProperties.getUserHeaderName(), jwtHelper.generateJwt(login)));
        anchorId.ifPresent(string -> requestBuilder.param("anchorId", string));
        contextSize.ifPresent(integer -> requestBuilder.param("contextSize", String.valueOf(integer)));
        limit.ifPresent(integer -> requestBuilder.param("limit", String.valueOf(integer)));
        search.ifPresent(string -> requestBuilder.param("search", string));
        return executeHttpRequest(
                new TypeReference<>() {
                },
                mockMvc,
                resultMatcher,
                userInfo,
                impersonateUserInfo,
                requestBuilder
        );
    }

    /**
     * Find local group by id
     *
     * @param mockMvc
     * @param resultMatcher
     * @param userInfo
     * @param localGroupId
     * @return
     * @throws Exception
     */
    public ApiResultResponse<LocalGroupDTO> v2AuthorizationControllerFindLocalGroupById(
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            Optional<String> userInfo,
            Optional<String> impersonateUserInfo,
            String localGroupId
    ) throws Exception {
        MockHttpServletRequestBuilder requestBuilder =
                get
                        (
                                "/v2/auth/local/group/{localGroupId}", localGroupId
                        )
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON);
        userInfo.ifPresent(login -> requestBuilder.header(appProperties.getUserHeaderName(), jwtHelper.generateJwt(login)));
        return executeHttpRequest(
                new TypeReference<>() {
                },
                mockMvc,
                resultMatcher,
                userInfo,
                impersonateUserInfo,
                requestBuilder
        );
    }

    public ApiResultResponse<Boolean> v2AuthorizationControllerManageGroupManagementAuthorization(
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            Optional<String> userInfo,
            Optional<String> impersonateUserInfo,
            AuthorizationGroupManagementDTO authorizationGroupManagementDTO) throws Exception {
        MockHttpServletRequestBuilder requestBuilder =
                post
                        (
                                "/v2/auth/local/group/authorize"
                        )
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                objectMapper.writeValueAsString(
                                        authorizationGroupManagementDTO
                                )
                        );
        userInfo.ifPresent(login -> requestBuilder.header(appProperties.getUserHeaderName(), jwtHelper.generateJwt(login)));
        return executeHttpRequest(
                new TypeReference<>() {
                },
                mockMvc,
                resultMatcher,
                userInfo,
                impersonateUserInfo,
                requestBuilder
        );
    }

    public ApiResultResponse<List<UserGroupManagementAuthorizationLevel>> v2AuthorizationControllerGetGroupManagementAuthorization(
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            Optional<String> userInfo,
            Optional<String> impersonateUserInfo,
            List<String> userIds) throws Exception {
        MockHttpServletRequestBuilder requestBuilder =
                get
                        (
                                "/v2/auth/local/group/authorize/{userIds}", String.join(",", userIds)
                        )
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON);
        userInfo.ifPresent(login -> requestBuilder.header(appProperties.getUserHeaderName(), jwtHelper.generateJwt(login)));
        // create a ';' separated string with all the user ids ans associate it to the userIds path variable

        return executeHttpRequest(
                new TypeReference<>() {
                },
                mockMvc,
                resultMatcher,
                userInfo,
                impersonateUserInfo,
                requestBuilder
        );
    }

    public <T> ApiResultResponse<T> executeHttpRequest(
            TypeReference<ApiResultResponse<T>> typeRef,
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            Optional<String> userInfo,
            Optional<String> impersonateUserInfo,
            MockHttpServletRequestBuilder requestBuilder) throws Exception {
        userInfo.ifPresent(login -> requestBuilder.header(appProperties.getUserHeaderName(), jwtHelper.generateJwt(login)));
        impersonateUserInfo.ifPresent(impersonateUser -> requestBuilder.header(appProperties.getImpersonateHeaderName(), impersonateUser));
        MvcResult result = mockMvc.perform(requestBuilder)
                .andExpect(resultMatcher)
                .andReturn();

        if (result.getResolvedException() != null) {
            throw result.getResolvedException();
        }
        return objectMapper.readValue(result.getResponse().getContentAsString(), typeRef);
    }
}
