package edu.stanford.slac.ad.eed.base_mongodb_lib.service;

import edu.stanford.slac.ad.eed.base_mongodb_lib.repository.AuthenticationTokenRepository;
import edu.stanford.slac.ad.eed.base_mongodb_lib.repository.AuthorizationRepository;
import edu.stanford.slac.ad.eed.base_mongodb_lib.repository.LocalGroupRepository;
import edu.stanford.slac.ad.eed.baselib.api.v1.dto.*;
import edu.stanford.slac.ad.eed.baselib.api.v1.mapper.AuthMapper;
import edu.stanford.slac.ad.eed.baselib.api.v2.dto.*;
import edu.stanford.slac.ad.eed.baselib.api.v2.mapper.LocalGroupMapper;
import edu.stanford.slac.ad.eed.baselib.auth.JWTHelper;
import edu.stanford.slac.ad.eed.baselib.config.AppProperties;
import edu.stanford.slac.ad.eed.baselib.exception.*;
import edu.stanford.slac.ad.eed.baselib.model.AuthenticationToken;
import edu.stanford.slac.ad.eed.baselib.model.Authorization;
import edu.stanford.slac.ad.eed.baselib.model.AuthorizationOwnerType;
import edu.stanford.slac.ad.eed.baselib.model.LocalGroup;
import edu.stanford.slac.ad.eed.baselib.service.AuthService;
import edu.stanford.slac.ad.eed.baselib.service.PeopleGroupService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationTypeDTO.Admin;
import static edu.stanford.slac.ad.eed.baselib.exception.Utility.assertion;
import static edu.stanford.slac.ad.eed.baselib.exception.Utility.wrapCatch;
import static edu.stanford.slac.ad.eed.baselib.utility.StringUtilities.normalizeStringWithReplace;

/**
 * This class is used to manage root user and root token.
 */
@Log4j2
@Service
public class AuthServiceImpl extends AuthService {
    @Value("${spring.application.name}")
    private String appName;
    private final JWTHelper jwtHelper;
    private final LocalGroupMapper localGroupMapper;
    private final AuthMapper authMapper;
    private final PeopleGroupService peopleGroupService;
    private final AppProperties appProperties;
    private final LocalGroupRepository localGroupRepository;
    private final AuthorizationRepository authorizationRepository;
    private final AuthenticationTokenRepository authenticationTokenRepository;

    /**
     * Constructor
     *
     * @param jwtHelper                     the jwt helper
     * @param authMapper                    the auth mapper
     * @param appProperties                 the app properties
     * @param peopleGroupService            the people group service
     * @param authorizationRepository       the authorization repository
     * @param authenticationTokenRepository the authentication token repository
     */
    public AuthServiceImpl(JWTHelper jwtHelper, LocalGroupMapper localGroupMapper, AuthMapper authMapper, AppProperties appProperties, PeopleGroupService peopleGroupService, LocalGroupRepository localGroupRepository, AuthorizationRepository authorizationRepository, AuthenticationTokenRepository authenticationTokenRepository) {
        super(appProperties);
        this.jwtHelper = jwtHelper;
        this.localGroupMapper = localGroupMapper;
        this.authMapper = authMapper;
        this.appProperties = appProperties;
        this.peopleGroupService = peopleGroupService;
        this.localGroupRepository = localGroupRepository;
        this.authorizationRepository = authorizationRepository;
        this.authenticationTokenRepository = authenticationTokenRepository;
    }

    /**
     * Delete an authorizations for a resource with a specific prefix
     *
     * @param resourcePrefix the prefix of the resource
     */
    public void deleteAuthorizationForResourcePrefix(String resourcePrefix) {
        wrapCatch(
                () -> {
                    authorizationRepository.deleteAllByResourceStartingWith(
                            resourcePrefix
                    );
                    return null;
                },
                -1,
                "AuthService::deleteAuthorizationResourcePrefix"
        );
    }

    @Override
    public void deleteAuthorizationForResourcePrefix(String resourcePrefix, String ownerId, AuthorizationOwnerTypeDTO ownerType) {
        wrapCatch(
                () -> {
                    authorizationRepository.deleteAllByResourceStartingWithAndOwnerIsAndOwnerTypeIs(
                            resourcePrefix,
                            ownerId,
                            authMapper.toModel(ownerType)
                    );
                    return null;
                },
                -1,
                "AuthService::deleteAuthorizationResourcePrefix"
        );
    }

    @Override
    public void deleteAuthorizationForResourcePrefix(String resourcePrefix, AuthorizationOwnerTypeDTO ownerType) {
        wrapCatch(
                () -> {
                    authorizationRepository.deleteAllByResourceStartingWithAndOwnerTypeIs(
                            resourcePrefix,
                            authMapper.toModel(ownerType)
                    );
                    return null;
                },
                -1,
                "AuthService::deleteAuthorizationResourcePrefix"
        );
    }

    /**
     * Delete an authorizations for a resource with a specific path
     *
     * @param resource the path of the resource
     */
    public void deleteAuthorizationForResource(String resource) {
        wrapCatch(
                () -> {
                    authorizationRepository.deleteAllByResourceIs(
                            resource
                    );
                    return null;
                },
                -1,
                "AuthService::deleteAuthorizationResourcePrefix"
        );
    }

    public List<AuthorizationDTO> getAllAuthorizationForOwnerAndAndAuthTypeAndResourcePrefix(
            String owner,
            AuthorizationTypeDTO authorizationType,
            String resourcePrefix) {
        return getAllAuthorizationForOwnerAndAndAuthTypeAndResourcePrefix(
                owner,
                authorizationType,
                resourcePrefix,
                Optional.empty()
        );
    }

    /**
     * Return all the authorizations for an owner that match with the prefix
     * and the authorizations type, if the owner is of type 'User' will be checked for all the
     * entries all along with those that belongs to all the user groups.
     *
     * @param ownerId                       si the owner target of the result authorizations
     * @param authorizationType           filter on the @Authorization.Type
     * @param resourcePrefix              is the prefix of the authorized resource
     * @param allHigherAuthOnSameResource return only the higher authorization for each resource
     * @return the list of found resource
     */
    @Override
    public List<AuthorizationDTO> getAllAuthorizationForOwnerAndAndAuthTypeAndResourcePrefix(String ownerId, AuthorizationTypeDTO authorizationType, String resourcePrefix, Optional<Boolean> allHigherAuthOnSameResource, Optional<Boolean> includeGroupForUser) {
        String realOwnerId = returnRealId(ownerId);
        // get user authorizations
        List<AuthorizationDTO> allAuth = new ArrayList<>(
                wrapCatch(
                        () -> authorizationRepository.findByOwnerAndOwnerTypeAndAuthorizationTypeIsGreaterThanEqualAndResourceStartingWith(
                                realOwnerId,
                                realOwnerId.contains("@") ? AuthorizationOwnerType.User:AuthorizationOwnerType.Token,
                                authMapper.toModel(authorizationType).getValue(),
                                resourcePrefix
                        ),
                        -1,
                        "AuthService::getAllAuthorization"
                ).stream().map(
                        authMapper::fromModel
                ).toList()
        );

        // get user authorizations inherited by group
        if (includeGroupForUser.isPresent() && includeGroupForUser.get().booleanValue()) {
            getAuthByGroupInheritance(realOwnerId, allAuth);
        }
        if (allHigherAuthOnSameResource.isPresent() && allHigherAuthOnSameResource.get()) {
            allAuth = allAuth.stream()
                    .collect(
                            Collectors.toMap(
                                    AuthorizationDTO::resource,
                                    auth -> auth,
                                    (existing, replacement) ->

                                            authMapper.toModel(existing.authorizationType()).getValue() >= authMapper.toModel(replacement.authorizationType()).getValue() ? existing : replacement
                            ))
                    .values().stream().toList();
        }
        return allAuth;
    }

    @Override
    public List<AuthorizationDTO> getAllAuthenticationForOwner(String owner, AuthorizationOwnerTypeDTO ownerType, String resourcePrefix, Optional<Boolean> allHigherAuthOnSameResource) {
        // get user authorizations
        List<AuthorizationDTO> allAuth = new ArrayList<>(
                wrapCatch(
                        () -> authorizationRepository.findByOwnerAndOwnerTypeIsAndResourceStartingWith(
                                owner,
                                authMapper.toModel(ownerType),
                                resourcePrefix
                        ),
                        -1,
                        "AuthService::getAllAuthorization"
                ).stream().map(
                        authMapper::fromModel
                ).toList()
        );

        // get user authorizations inherited by group
        getAuthByGroupInheritance(owner, allAuth);

        if (allHigherAuthOnSameResource.isPresent() && allHigherAuthOnSameResource.get()) {
            allAuth = allAuth.stream()
                    .collect(
                            Collectors.toMap(
                                    AuthorizationDTO::resource,
                                    auth -> auth,
                                    (existing, replacement) ->

                                            authMapper.toModel(existing.authorizationType()).getValue() >= authMapper.toModel(replacement.authorizationType()).getValue() ? existing : replacement
                            ))
                    .values().stream().toList();
        }
        return allAuth;
    }

    @Override
    public List<AuthorizationDTO> getAllAuthenticationForOwner(String owner, AuthorizationOwnerTypeDTO ownerType, Optional<Boolean> allHigherAuthOnSameResource, Optional<Boolean> includeInherited) {
        // get user authorizations
        List<AuthorizationDTO> allAuth = new ArrayList<>(
                wrapCatch(
                        () -> authorizationRepository.findByOwnerAndOwnerTypeIs(
                                owner,
                                authMapper.toModel(ownerType)
                        ),
                        -1,
                        "AuthService::getAllAuthorization"
                ).stream().map(
                        authMapper::fromModel
                ).toList()
        );

        // get user authorizations inherited by group
        if (includeInherited.isPresent() && includeInherited.get()) {
            getAuthByGroupInheritance(owner, allAuth);
        }

        if (allHigherAuthOnSameResource.isPresent() && allHigherAuthOnSameResource.get()) {
            allAuth = allAuth.stream()
                    .collect(
                            Collectors.toMap(
                                    AuthorizationDTO::resource,
                                    auth -> auth,
                                    (existing, replacement) -> authMapper.toModel(existing.authorizationType()).getValue() >= authMapper.toModel(replacement.authorizationType()).getValue() ? existing : replacement
                            ))
                    .values().stream().toList();
        }

        return allAuth;
    }

    /**
     * Return all the authorizations for an owner that match with the prefix
     * and the authorizations type, if the owner is of type 'User' will be checked for all the
     * entries all along with those that belongs to all the user groups.
     *
     * @param ownerId is the owner target of the result authorizations
     * @param allAuth the container that host all the user authorizations
     * @return the list of found resource
     */
    private void getAuthByGroupInheritance(String ownerId, List<AuthorizationDTO> allAuth) {
        // in case we have a user check also the groups that belongs to the user
        List<String> userGroups = getGroupByUserId(ownerId);

        // load all groups authorizations
        allAuth.addAll(
                userGroups
                        .stream()
                        .map(
                                groupId -> authorizationRepository.findByOwnerAndOwnerTypeIs(
                                        groupId,
                                        AuthorizationOwnerType.Group

                                )
                        )
                        .flatMap(List::stream)
                        .map(
                                authMapper::fromModel
                        )
                        .toList()
        );
    }

    /**
     * Return all the groups where the user belongs
     *
     * @param ownerId the user id
     * @return
     */
    private List<String> getGroupByUserId(String ownerId) {
        List<String> userGroups = new ArrayList<>();
        // in case we have a user check also the groups that belongs to the user
        userGroups.addAll(
                peopleGroupService.findGroupByUserId(ownerId)
                        .stream()
                        .map(GroupDTO::uid)
                        .toList()
        );

        // get all local group
        userGroups.addAll(
                localGroupRepository.findAllByMembersContains(ownerId)
                        .stream()
                        .map(LocalGroup::getId)
                        .toList()
        );
        return userGroups;
    }

    /**
     * Update all configured root user
     */
    public void updateRootUser() {
        log.info("Find current authorizations");
        // remove internal service email from the root user list, they are managed automatically in the token list
        List<String> rootUserList = appProperties.getRootUserList().stream().filter(
                email -> !email.contains("@internal.")
        ).toList();
        //load actual root
        List<Authorization> currentRootUser = wrapCatch(
                () -> authorizationRepository.findByResourceIsAndAuthorizationTypeIsGreaterThanEqualAndOwnerTypeIs(
                        "*",
                        authMapper.toModel(Admin).getValue(),
                        AuthorizationOwnerType.User
                ),
                -1,
                "AuthService::updateRootUser"
        );

        currentRootUser.addAll(
                wrapCatch(
                        () -> authorizationRepository.findByResourceIsAndAuthorizationTypeIsGreaterThanEqualAndOwnerTypeIs(
                                "*",
                                authMapper.toModel(Admin).getValue(),
                                AuthorizationOwnerType.Token
                        ),
                        -2,
                        "AuthService::updateRootUser"
                ).stream().filter(
                        // add also the internal service token that are root
                        auth -> appProperties.isServiceInternalTokenEmail(auth.getOwner())
                ).toList()
        );

        // find root users to remove
        List<String> rootUserToRemove = currentRootUser.stream().map(
                Authorization::getOwner
        ).toList().stream().filter(
                userEmail -> !wrapCatch(
                        () -> rootUserList.contains(userEmail),
                        -2,
                        "AuthService::updateRootUser"
                )
        ).toList();

        for (String userEmailToRemove :
                rootUserToRemove) {
            log.info("Remove root authorizations: {}", userEmailToRemove);
            wrapCatch(
                    () -> {
                        authorizationRepository.deleteByOwnerIsAndResourceIsAndAuthorizationTypeIs(
                                userEmailToRemove,
                                "*",
                                authMapper.toModel(Admin).getValue()
                        );
                        return null;
                    },
                    -2,
                    "AuthService::updateRootUser"
            );
        }

        // ensure current root users
        log.info("Ensure root authorizations for: {}", rootUserList);
        for (String userEmail :
                rootUserList) {
            // find root authorizations for user email
            Optional<Authorization> rootAuth = wrapCatch(
                    () -> authorizationRepository.findByOwnerIsAndResourceIsAndAuthorizationTypeIsGreaterThanEqual(
                            userEmail,
                            "*",
                            authMapper.toModel(Admin).getValue()
                    ),
                    -2,
                    "AuthService::updateRootUser"
            );
            if (rootAuth.isEmpty()) {
                log.info("Create root authorizations for user '{}'", userEmail);
                var isServiceInternalEmail = appProperties.isServiceInternalTokenEmail(userEmail);
                wrapCatch(
                        () -> authorizationRepository.save(
                                Authorization
                                        .builder()
                                        .authorizationType(authMapper.toModel(Admin).getValue())
                                        .owner(userEmail)
                                        .ownerType(isServiceInternalEmail ? AuthorizationOwnerType.Token : AuthorizationOwnerType.User)
                                        .resource("*")
                                        .creationBy(appProperties.getAppName())
                                        .build()
                        ),
                        -2,
                        "AuthService::updateRootUser"
                );
            } else {
                log.info("Root authorizations for '{}' already exists", userEmail);
            }
        }
    }

    /**
     * Update all configured root token
     */
    @Transactional
    public void updateAutoManagedRootToken() {
        log.info("Find current authentication token app managed");

        log.info("Find root user that need to became a another microservice root token");
        List<String> rootMicroservicesEmails = appProperties.getRootUserList().stream().filter(
                email->email.contains("@internal.")
        ).toList();
        for (String rootMicroserviceEmail :
                rootMicroservicesEmails) {
            log.info("Prefill token description for microservice email {}", rootMicroserviceEmail);
            appProperties.getRootAuthenticationTokenList().add(
                    NewAuthenticationTokenDTO
                            .builder()
                            .name(rootMicroserviceEmail)
                            .expiration(LocalDate.now().plus(1, java.time.temporal.ChronoUnit.YEARS))
                            .build()
            );
        }

        // now remove token for internal service no more needed
        appProperties.getRootAuthenticationTokenList().removeIf(
                token -> token.name().contains("@internal.") && !rootMicroservicesEmails.contains(token.name())
        );

        //load actual root
        if (appProperties.getRootAuthenticationTokenList().isEmpty()) {
            List<AuthenticationToken> foundAuthenticationTokens = wrapCatch(
                    () -> wrapCatch(
                            authenticationTokenRepository::findAllByApplicationManagedIsTrue,
                            -1,
                            "AuthService::updateAutoManagedRootToken"
                    ),
                    -2,
                    "AuthService::updateAutoManagedRootToken"
            );
            for (AuthenticationToken authToken :
                    foundAuthenticationTokens) {

                wrapCatch(
                        () -> {
                            authorizationRepository.deleteAllByOwnerIs(authToken.getId());
                            return null;
                        },
                        -3,
                        "AuthService::updateAutoManagedRootToken"
                );

                wrapCatch(
                        () -> {
                            authenticationTokenRepository.deleteById(
                                    authToken.getId()
                            );
                            return null;
                        },
                        -3,
                        "AuthService::updateAutoManagedRootToken"
                );
            }
            wrapCatch(
                    () -> {
                        authenticationTokenRepository.deleteAllByApplicationManagedIsTrue();
                        return null;
                    },
                    -4,
                    "AuthService::updateAutoManagedRootToken"
            );
            return;
        }
        List<AuthenticationToken> foundAuthenticationTokens = wrapCatch(
                authenticationTokenRepository::findAllByApplicationManagedIsTrue,
                -5,
                "AuthService::updateAutoManagedRootToken"
        );

        // check which we need to create
        for (
                NewAuthenticationTokenDTO newAuthenticationTokenDTO :
                appProperties.getRootAuthenticationTokenList()
        ) {
            var toCreate = foundAuthenticationTokens
                    .stream()
                    .filter(t -> t.getName().compareToIgnoreCase(newAuthenticationTokenDTO.name()) == 0)
                    .findAny().isEmpty();
            if (toCreate) {
                var newAuthTok = wrapCatch(
                        () -> authenticationTokenRepository.save(
                                getAuthenticationToken(
                                        newAuthenticationTokenDTO,
                                        (newAuthenticationTokenDTO.name().contains("@internal.") ? newAuthenticationTokenDTO.name() : null),
                                        true
                                )
                        ),
                        -6,
                        "AuthService::updateAutoManagedRootToken"
                );
                log.info("Created authentication token with name {}", newAuthTok.getName());

                wrapCatch(
                        () -> authorizationRepository.save(
                                Authorization
                                        .builder()
                                        .authorizationType(authMapper.toModel(Admin).getValue())
                                        .owner(newAuthTok.getId())
                                        .ownerType(AuthorizationOwnerType.Token)
                                        .resource("*")
                                        .creationBy(appName)
                                        .build()
                        ),
                        -7,
                        "AuthService::updateAutoManagedRootToken"
                );

                log.info("Created root authorization for token with name {}", newAuthTok.getName());
            }
        }

        // check which we need to remove
        for (
                AuthenticationToken foundAuthenticationToken :
                foundAuthenticationTokens
        ) {
            var toDelete = appProperties.getRootAuthenticationTokenList()
                    .stream()
                    .filter(t -> t.name().compareToIgnoreCase(foundAuthenticationToken.getName()) == 0)
                    .findAny().isEmpty();
            if (toDelete) {
                log.info("Delete authentication token for id {}", foundAuthenticationToken.getName());
                wrapCatch(
                        () -> {
                            deleteToken(
                                    foundAuthenticationToken.getId()
                            );
                            return null;
                        },
                        -8,
                        "AuthService::updateAutoManagedRootToken"
                );
            }
        }
    }

    @Override
    public void addRootAuthorization(String ownerId, String creator) {
        boolean isUser = ownerId.contains("@");
        // check fi the user or app token exists
        if (!isUser) {
            // try to check if the id is an application token
            var authenticationTokenFound = authenticationTokenRepository
                    .findById(ownerId)
                    .orElseThrow(
                            () -> AuthenticationTokenNotFound.authTokenNotFoundBuilder()
                                    .errorCode(-2)
                                    .errorDomain("AuthService::addRootAuthorization")
                                    .build()
                    );
            assertion(
                    ControllerLogicException
                            .builder()
                            .errorCode(-3)
                            .errorMessage("Authentication Token managed by the application cannot be managed by user")
                            .errorDomain("AuthService::addRootAuthorization")
                            .build(),
                    // should be not an application managed app token
                    () -> !authenticationTokenFound.getApplicationManaged()
            );
        } else {
            // find the user
            assertion(
                    () -> peopleGroupService.findPersonByEMail(ownerId) != null,
                    PersonNotFound.personNotFoundBuilder()
                            .errorCode(-4)
                            .errorDomain("AuthService::addRootAuthorization")
                            .build()
            );
        }

        // check if root authorization is already been granted
        Optional<Authorization> rootAuth = authorizationRepository.findByOwnerIsAndResourceIsAndAuthorizationTypeIsGreaterThanEqual(
                ownerId,
                "*",
                authMapper.toModel(Admin).getValue()
        );
        if (rootAuth.isPresent()) return;
        wrapCatch(
                () -> authorizationRepository.save(
                        Authorization
                                .builder()
                                .authorizationType(authMapper.toModel(Admin).getValue())
                                .owner(ownerId)
                                .ownerType(isUser ? AuthorizationOwnerType.User:AuthorizationOwnerType.Token)
                                .resource("*")
                                .creationBy(creator)
                                .build()
                ),
                -1,
                "AuthService::addRootAuthorization"
        );
    }

    /**
     * Add user identified by email as root
     *
     * @param email the user email
     */
    public void addRootAuthorization(String email, String creator, String appName) {
        boolean isAuthToken = appProperties.isAuthenticationToken(email);

        // check fi the user or app token exists
        if (isAuthToken) {
            // give error in case of a logbook token(it cannot be root
            assertion(
                    ControllerLogicException.builder()
                            .errorCode(-1)
                            .errorMessage("Application specific token cannot became root")
                            .errorDomain("AuthService::addRootAuthorization")
                            .build(),
                    () -> !appProperties.isAppTokenEmail(email)
            );
            // create root for global token
            var authenticationTokenFound = authenticationTokenRepository
                    .findByEmailIs(email)
                    .orElseThrow(
                            () -> AuthenticationTokenNotFound.authTokenNotFoundBuilder()
                                    .errorCode(-1)
                                    .errorDomain("AuthService::addRootAuthorization")
                                    .build()
                    );
            assertion(
                    ControllerLogicException
                            .builder()
                            .errorCode(-1)
                            .errorMessage("Authentication Token managed byt the elog application cannot be managed by user")
                            .errorDomain("AuthService::addRootAuthorization")
                            .build(),
                    // should be not an application managed app token
                    () -> !authenticationTokenFound.getApplicationManaged()
            );
        } else {
            // find the user
            assertion(
                    () -> peopleGroupService.findPersonByEMail(email) != null,
                    PersonNotFound.personNotFoundBuilder()
                            .errorCode(-4)
                            .errorDomain("AuthService::addRootAuthorization")
                            .build()
            );
        }

        // check if root authorization is already benn granted
        Optional<Authorization> rootAuth = authorizationRepository.findByOwnerIsAndResourceIsAndAuthorizationTypeIsGreaterThanEqual(
                email,
                "*",
                authMapper.toModel(Admin).getValue()
        );
        if (rootAuth.isPresent()) return;
        wrapCatch(
                () -> authorizationRepository.save(
                        Authorization
                                .builder()
                                .authorizationType(authMapper.toModel(Admin).getValue())
                                .owner(email)
                                .ownerType(isAuthToken ? AuthorizationOwnerType.Token : AuthorizationOwnerType.User)
                                .resource("*")
                                .creationBy(creator)
                                .build()
                ),
                -1,
                "AuthService::addRootAuthorization"
        );
    }

    /**
     * Remove user identified by email as root user
     *
     * @param ownerId that identify the user
     */
    public void removeRootAuthorization(String ownerId) {
        boolean isUser = appProperties.isAuthenticationToken(ownerId);
        if (!isUser) {
            // check if the authentication token exists before remove
            var authenticationTokenFound = authenticationTokenRepository
                    .findById(ownerId)
                    .orElseThrow(
                            () -> AuthenticationTokenNotFound.authTokenNotFoundBuilder()
                                    .errorCode(-1)
                                    .errorDomain("AuthService::removeRootAuthorization")
                                    .build()
                    );
            assertion(
                    ControllerLogicException
                            .builder()
                            .errorCode(-1)
                            .errorMessage("Authentication Token managed byt the elog application cannot be managed by user")
                            .errorDomain("AuthService::removeRootAuthorization")
                            .build(),
                    // should be not an application managed app token
                    () -> !authenticationTokenFound.getApplicationManaged()
            );
        }
        Optional<Authorization> rootAuth = authorizationRepository.findByOwnerIsAndResourceIsAndAuthorizationTypeIsGreaterThanEqual(
                ownerId,
                "*",
                authMapper.toModel(Admin).getValue()
        );
        if (rootAuth.isEmpty()) return;

        wrapCatch(
                () -> {
                    authorizationRepository.delete(rootAuth.get());
                    return null;
                },
                -1,
                "AuthService::removeRootAuthorization"
        );
    }

    /**
     * Return all root authorization
     *
     * @return all the root authorization
     */
    public List<AuthorizationDTO> findAllRoot() {
        return wrapCatch(
                () -> authorizationRepository.findByResourceIs("*"),
                -1,
                "AuthService::findAllRoot"
        )
                .stream()
                .map(
                        authMapper::fromModel
                )
                .toList();
    }

    @Override
    public String updateAuthorizationType(String authorizationId, AuthorizationTypeDTO authorizationTypeDTO) {
        var foundAuthorization = wrapCatch(
                () -> authorizationRepository.findById(authorizationId)
                        .orElseThrow(
                                () -> AuthenticationTokenNotFound.authTokenNotFoundBuilder()
                                        .errorCode(-1)
                                        .errorDomain("AuthService::updateAuthorizationType")
                                        .build()
                        ),
                -1
        );

        var updatedAuthorization = authorizationRepository.save(
                foundAuthorization.toBuilder()
                        .authorizationType(
                                authMapper.toModel(authorizationTypeDTO).getValue()
                        )
                        .build()
        );
        return updatedAuthorization.getId();
    }

    @Override
    public String ensureAuthorization(AuthorizationDTO authorizationDTO) {
        return wrapCatch(
                () -> authorizationRepository.ensureAuthorization(authMapper.toModel(authorizationDTO)),
                -1,
                "AuthService::addNewAuthorization"
        );
    }

    @Override
    public String addNewAuthorization(NewAuthorizationDTO newAuthorizationDTO) {
        var result = wrapCatch(
                () -> authorizationRepository.save(authMapper.toModel(newAuthorizationDTO)),
                -1,
                "AuthService::addNewAuthorization"
        );
        return result.getId();
    }

    @Override
    public AuthorizationDTO findAuthorizationById(String authorizationId) {
        return wrapCatch(
                () -> authorizationRepository.findById(authorizationId)
                        .map(authMapper::fromModel)
                        .orElseThrow(
                                () -> AuthorizationNotFound.authorizationNotFound()
                                        .errorCode(-1)
                                        .authId(authorizationId)
                                        .build()
                        ),
                -2,
                "AuthService::addNewAuthorization"
        );
    }

    @Override
    public void deleteAuthorizationById(String authorizationId) {
        wrapCatch(
                () -> {
                    authorizationRepository.deleteById(authorizationId);
                    return null;
                },
                -1,
                "AuthService::deleteAuthorizationById"
        );
    }

    @Override
    public List<AuthorizationDTO> findByResourceIs(String resource) {
        return wrapCatch(
                () -> authorizationRepository.findByResourceIs(resource),
                -1,
                "AuthService::findByResourceIs"
        ).stream().map(
                authMapper::fromModel
        ).toList();
    }

    /**
     * Ensure token
     *
     * @param authenticationToken token to ensure
     */
    public String ensureAuthenticationToken(AuthenticationToken authenticationToken) {
        Optional<AuthenticationToken> token = wrapCatch(
                () -> authenticationTokenRepository.findByEmailIs(authenticationToken.getEmail()),
                -1,
                "AuthService:ensureAuthenticationToken"
        );
        if (token.isPresent()) return token.get().getId();

        authenticationToken.setName(
                normalizeStringWithReplace(
                        authenticationToken.getName(),
                        " ",
                        "-"
                )
        );

        authenticationToken.setToken(
                jwtHelper.generateAuthenticationToken(
                        authenticationToken
                )
        );
        AuthenticationToken newToken = wrapCatch(
                () -> authenticationTokenRepository.save(
                        authenticationToken
                ),
                -2,
                "AuthService:ensureAuthenticationToken"
        );
        return newToken.getId();
    }

    public AuthenticationTokenDTO addNewAuthenticationToken(NewAuthenticationTokenDTO newAuthenticationTokenDTO) {
        return addNewAuthenticationToken(newAuthenticationTokenDTO, false);
    }

    /**
     * Add a new authentication token
     *
     * @param newAuthenticationTokenDTO is the new token information
     */
    public AuthenticationTokenDTO addNewAuthenticationToken(NewAuthenticationTokenDTO newAuthenticationTokenDTO, boolean appManaged) {
        // check if a token with the same name already exists
        assertion(
                AuthenticationTokenMalformed.malformedAuthToken()
                        .errorCode(-1)
                        .errorDomain("AuthService::addNewAuthenticationToken")
                        .build(),
                // name well-formed
                () -> newAuthenticationTokenDTO.name() != null && !newAuthenticationTokenDTO.name().isEmpty(),
                // expiration well-formed
                () -> newAuthenticationTokenDTO.expiration() != null
        );

        assertion(
                () -> wrapCatch(
                        () -> !authenticationTokenRepository.existsByName(newAuthenticationTokenDTO.name()),
                        -2,
                        "AuthService::addNewAuthenticationToken"
                ),
                ControllerLogicException
                        .builder()
                        .errorCode(-3)
                        .errorMessage("A token with the same name already exists")
                        .errorDomain("AuthService::addNewAuthenticationToken")
                        .build()
        );
        // convert to model and normalize the name
        return authMapper.toTokenDTO(
                wrapCatch(
                        () -> authenticationTokenRepository.save(
                                getAuthenticationToken(
                                        newAuthenticationTokenDTO,
                                        appManaged
                                )
                        ),
                        -4,
                        "AuthService::addNewAuthenticationToken"
                )
        );
    }

    @Override
    public AuthenticationTokenDTO addNewApplicationAuthenticationToken(NewAuthenticationTokenDTO newAuthenticationTokenDTO, boolean appManaged) {
        // check if a token with the same name already exists
        assertion(
                AuthenticationTokenMalformed.malformedAuthToken()
                        .errorCode(-1)
                        .errorDomain("AuthService::addNewApplicationAuthenticationToken")
                        .build(),
                // name well-formed
                () -> newAuthenticationTokenDTO.name() != null && !newAuthenticationTokenDTO.name().isEmpty(),
                // expiration well-formed
                () -> newAuthenticationTokenDTO.expiration() != null
        );

        assertion(
                () -> wrapCatch(
                        () -> !authenticationTokenRepository.existsByName(newAuthenticationTokenDTO.name()),
                        -2,
                        "AuthService::addNewApplicationAuthenticationToken"
                ),
                ControllerLogicException
                        .builder()
                        .errorCode(-3)
                        .errorMessage("A token with the same name already exists")
                        .errorDomain("AuthService::addNewApplicationAuthenticationToken")
                        .build()
        );
        // convert to model and normalize the name
        return authMapper.toTokenDTO(
                wrapCatch(
                        () -> authenticationTokenRepository.save(
                                getApplicationAuthenticationToken(
                                        newAuthenticationTokenDTO,
                                        appManaged
                                )
                        ),
                        -4,
                        "AuthService::addNewAuthenticationToken"
                )
        );
    }

    private AuthenticationToken getApplicationAuthenticationToken(NewAuthenticationTokenDTO newAuthenticationTokenDTO, boolean appManaged) {
        AuthenticationToken authTok = authMapper.toModelApplicationToken(
                newAuthenticationTokenDTO.toBuilder()
                        .name(
                                normalizeStringWithReplace(
                                        newAuthenticationTokenDTO.name(),
                                        " ",
                                        "-"
                                )
                        )
                        .build()
        );
        return authTok.toBuilder()
                .applicationManaged(appManaged)
                .token(
                        jwtHelper.generateAuthenticationToken(
                                authTok
                        )
                )
                .build();
    }

    /**
     * Add a new authentication token
     *
     * @param newAuthenticationTokenDTO is the new token information
     */
    private AuthenticationToken getAuthenticationToken(NewAuthenticationTokenDTO newAuthenticationTokenDTO, boolean appManaged) {
        return getAuthenticationToken(newAuthenticationTokenDTO, null, appManaged);
    }

    /**
     * Add a new authentication token
     *
     * @param newAuthenticationTokenDTO is the new token information
     */
    private AuthenticationToken getAuthenticationToken(NewAuthenticationTokenDTO newAuthenticationTokenDTO, String prefilledEmail, boolean appManaged) {
        log.info("Create new authentication token with name {}", newAuthenticationTokenDTO.name());
        AuthenticationToken authTok = authMapper.toModelApplicationToken(
                newAuthenticationTokenDTO.toBuilder()
                        .name(
                                normalizeStringWithReplace(
                                        newAuthenticationTokenDTO.name(),
                                        " ",
                                        "-"
                                )
                        )
                        .build()
        );
        // temp token to manage the emil and app managed information that could be fall into the jwt generation
        var tempToken =  authTok.toBuilder()
                .applicationManaged(appManaged)
                .email(prefilledEmail != null ? prefilledEmail : authTok.getEmail())
                .build();
        log.info("Generate token for authentication token with name {} with values", newAuthenticationTokenDTO.name(), tempToken);
        // generate jwt and return the newly created token
        String generatedToken = jwtHelper.generateAuthenticationToken(tempToken);
        log.info("Generated token for authentication token with name {} is {}", newAuthenticationTokenDTO.name(), generatedToken);
        return tempToken.toBuilder()
                .token(generatedToken)
                .build();
    }

    /**
     * Return an application token by name
     *
     * @param name the name of the token to return
     * @return the found authentication token
     */
    public Optional<AuthenticationTokenDTO> getAuthenticationTokenByName(String name) {
        return wrapCatch(
                () -> authenticationTokenRepository.findByName(name)
                        .map(
                                authMapper::toTokenDTO
                        ),
                -1,
                "AuthService::getAuthenticationTokenByName"
        );
    }

    /**
     * return al the global authentication tokens
     *
     * @return the list of all authentication tokens
     */
    public List<AuthenticationTokenDTO> getAllAuthenticationToken() {
        return wrapCatch(
                () -> authenticationTokenRepository.findAll()
                        .stream()
                        .map(
                                authMapper::toTokenDTO
                        ).toList(),
                -1,
                "AuthService::getAuthenticationTokenByName"
        );
    }

    @Override
    public List<AuthenticationTokenDTO> findAllAuthenticationToken(AuthenticationTokenQueryParameterDTO queryParameterDTO) {
        return wrapCatch(
                () -> authenticationTokenRepository.findAll(
                        authMapper.toModel(queryParameterDTO)
                ).stream().map(
                        authMapper::toTokenDTO
                ).toList(),
                -1
        );
    }

    /**
     * Delete a token by name along with all his authorization records
     *
     * @param tokenId the token id
     */
    @Transactional
    public void deleteToken(String tokenId) {
        AuthenticationTokenDTO tokenToDelete = getAuthenticationTokenById(tokenId)
                .orElseThrow(
                        () -> AuthenticationTokenNotFound.authTokenNotFoundBuilder()
                                .errorCode(-1)
                                .errorDomain("AuthService::deleteToken")
                                .build()
                );
        // delete token
        wrapCatch(
                () -> {
                    authenticationTokenRepository.deleteById(tokenToDelete.id());
                    return null;
                },
                -3,
                "AuthService::deleteToken"
        );
        //delete authorizations
        wrapCatch(
                () -> {
                    authorizationRepository.deleteAllByOwnerIs(tokenToDelete.id());
                    return null;
                },
                -2,
                "AuthService::deleteToken"
        );
    }

    /**
     * Return an application token by name
     *
     * @param id the unique id of the token to return
     * @return the found authentication token
     */
    public Optional<AuthenticationTokenDTO> getAuthenticationTokenById(String id) {
        return wrapCatch(
                () -> authenticationTokenRepository.findById(id)
                        .map(
                                authMapper::toTokenDTO
                        ),
                -1,
                "AuthService::getAuthenticationTokenByName"
        );
    }

    /**
     * @param email
     * @return
     */
    public boolean existsAuthenticationTokenByEmail(String email) {
        return wrapCatch(
                () -> authenticationTokenRepository.existsByEmail(email),
                -1,
                "AuthService::existsAuthenticationTokenByEmail"
        );
    }

    /**
     * Return the authentication token by email
     *
     * @param email the email of the authentication token to return
     * @return the authentication token found
     */
    public Optional<AuthenticationTokenDTO> getAuthenticationTokenByEmail(String email) {
        return wrapCatch(
                () -> authenticationTokenRepository.findByEmailIs(email),
                -1,
                "AuthService::existsAuthenticationTokenByEmail"
        ).map(
                authMapper::toTokenDTO
        );
    }

    /**
     * delete all the authorization where the email ends with the postfix
     *
     * @param emailPostfix the terminal string of the email
     */
    public void deleteAllAuthenticationTokenWithEmailEndWith(String emailPostfix) {
        wrapCatch(
                () -> {
                    authenticationTokenRepository.deleteAllByEmailEndsWith(emailPostfix);
                    return null;
                },
                -1,
                "AuthService::deleteAllAuthenticationTokenWithEmailEndWith"
        );
    }

    @Override
    public List<AuthenticationTokenDTO> getAuthenticationTokenByEmailEndsWith(String emailPostfix) {
        return wrapCatch(
                () -> authenticationTokenRepository.findAllByEmailEndsWith(emailPostfix),
                -1,
                "AuthService::getAuthenticationTokenByEmailEndsWith"
        ).stream().map(
                authMapper::toTokenDTO
        ).toList();
    }


    @Override
    public boolean canManageGroup(String userId) {
        boolean isAppToken = appProperties.isAuthenticationToken(userId);
        // get user authorizations
        List<AuthorizationDTO> allAuth = new ArrayList<>(
                wrapCatch(
                        () -> authorizationRepository.findByOwnerAndOwnerTypeAndAuthorizationTypeIsGreaterThanEqualAndResourceStartingWith(
                                userId,
                                isAppToken ? AuthorizationOwnerType.Token : AuthorizationOwnerType.User,
                                Authorization.Type.Admin.getValue(),
                                "%s/group".formatted(
                                        appProperties.getAppName()
                                )
                        ),
                        -1,
                        "AuthService::getAllAuthorization"
                ).stream().map(
                        authMapper::fromModel
                ).toList()
        );
        return !allAuth.isEmpty();
    }

    @Override
    public void authorizeUserIdToManageGroup(String userId) {
        boolean isAppToken = appProperties.isAuthenticationToken(userId);
        wrapCatch(
                () -> authorizationRepository.ensureAuthorization(
                        Authorization.builder()
                                .authorizationType(Authorization.Type.Admin.getValue())
                                .owner(userId)
                                .ownerType(isAppToken ? AuthorizationOwnerType.Token : AuthorizationOwnerType.User)
                                .resource("%s/group".formatted(
                                        appProperties.getAppName()
                                ))
                                .creationBy(appProperties.getAppName())
                                .build()
                ),
                -1,
                "AuthService::addNewAuthorization"
        );
    }

    @Override
    public void removeAuthorizationToUserIdToManageGroup(String userId) {
        wrapCatch(
                () -> {
                    authorizationRepository.deleteByOwnerIsAndResourceIsAndAuthorizationTypeIs(
                            userId,
                            "%s/group".formatted(
                                    appProperties.getAppName()
                            ),
                            Authorization.Type.Admin.getValue()
                    );
                    return null;
                },
                -1,
                "AuthService::addNewAuthorization"
        );
    }

    @Override
    public void manageAuthorizationOnGroup(AuthorizationGroupManagementDTO authorizationGroupManagementDTO) {
        if (authorizationGroupManagementDTO == null) return;

        // add new users
        if (authorizationGroupManagementDTO.addUsers() != null) {
            authorizationGroupManagementDTO.addUsers().forEach(u -> authorizeUserIdToManageGroup(u));
        }

        // remove users
        if (authorizationGroupManagementDTO.removeUsers() != null) {
            authorizationGroupManagementDTO.removeUsers().forEach(u -> removeAuthorizationToUserIdToManageGroup(u));
        }

    }

    @Override
    public List<UserGroupManagementAuthorizationLevel> getGroupManagementAuthorization(List<String> userIds) {
        List<Authorization> authFound = wrapCatch(
                () -> authorizationRepository.findByResourceIsAndOwnerIn("%s/group".formatted(appProperties.getAppName()), userIds),
                1
        );
        var result = authFound.stream().map(
                auth -> UserGroupManagementAuthorizationLevel.builder()
                        .user(peopleGroupService.findPersonByEMail(auth.getOwner()))
                        .canManageGroup(true)
                        .build()
        ).toList();
        // create a new editable list
        var mutableResult = new ArrayList<>(result);
        //add not found user with authorization at false
        userIds.stream().filter(
                u -> result.stream().noneMatch(
                        r -> r.user().mail().compareToIgnoreCase(u) == 0
                )
        ).forEach(
                u -> mutableResult.add(
                        UserGroupManagementAuthorizationLevel.builder()
                                .user(peopleGroupService.findPersonByEMail(u))
                                .canManageGroup(checkForRoot(new UsernamePasswordAuthenticationToken(u, null)))
                                .build()
                )
        );
        return mutableResult;
    }

    @Override
    public String createLocalGroup(NewLocalGroupDTO newGroupDTO) {
        // check for members
        if(newGroupDTO.members()!=null){
            // remove duplicates
            var newList = newGroupDTO.members().stream().distinct().toList();
            newList.forEach(peopleGroupService::findPersonByEMail);
            newGroupDTO = newGroupDTO.toBuilder().members(newList).build();
        }
        // save group
        LocalGroup localGroup = localGroupMapper.fromDTO(newGroupDTO);
        LocalGroup savedGroup = wrapCatch(
                () -> localGroupRepository.save(localGroup),
                -1,
                "AuthService::createLocalGroup"
        );
        return savedGroup.getId();
    }

    @Override
    public void updateLocalGroup(String localGroupId, UpdateLocalGroupDTO updateGroupDTO) {
        LocalGroup foundLocalGroup = wrapCatch(
                () -> localGroupRepository.findById(localGroupId),
                -1
        ).orElseThrow(
                () -> ControllerLogicException.builder()
                        .errorCode(-2)
                        .errorMessage("Local group of id %s not found".formatted(localGroupId))
                        .errorDomain("AuthService::updateLocalGroup")
                        .build()
        );
        // check for members
        if(updateGroupDTO.members()!=null){
            var newList = updateGroupDTO.members().stream().distinct().toList();
            newList.forEach(peopleGroupService::findPersonByEMail);
            updateGroupDTO = updateGroupDTO.toBuilder().members(newList).build();
        }
        UpdateLocalGroupDTO finalUpdateGroupDTO = updateGroupDTO;
        wrapCatch(
                () -> localGroupRepository.save(
                        localGroupMapper.updateModel(finalUpdateGroupDTO, foundLocalGroup)
                ),
                -3
        );
    }

    @Override
    public void deleteLocalGroup(String localGroupId) {
        wrapCatch(
                () -> {
                    localGroupRepository.deleteById(localGroupId);
                    return null;
                },
                -1
        );
    }

    @Override
    public List<LocalGroupDTO> findLocalGroup(LocalGroupQueryParameterDTO query) {
        return wrapCatch(
                () -> localGroupRepository.findAll(localGroupMapper.toQuery(query)),
                -1
        ).stream().map(
                localGroupMapper::toDTO
        ).toList();
    }

    @Override
    public LocalGroupDTO findLocalGroupById(String localGroupId) {
        return wrapCatch(
                () -> localGroupRepository.findById(localGroupId),
                -1
        ).map(
                localGroupMapper::toDTO
        ).orElseThrow(
                () -> GroupNotFound.groupNotFound()
                        .errorCode(-2)
                        .groupId(localGroupId)
                        .errorDomain("AuthService::findLocalGroupById")
                        .build()
        );
    }

    /**
     * Return the real id to use to search the authorizations
     * only in case of appToken it need to search for the id
     * @param ownerId the owner id
     * @return
     */
    private String returnRealId(String ownerId) {
        var authToken = getAuthenticationTokenByEmail(ownerId);
        if (authToken.isPresent()) {
            return authToken.get().id();
        }
        return ownerId;
    }
}
