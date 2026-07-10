package com.evolveum.midpoint.smart.api.conndev;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevAuthInfoType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevHttpAuthTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevIntegrationType;

import java.util.Arrays;
import java.util.List;

public enum SupportedAuthorization {
    NONE( null, new ConnDevAuthInfoType()
            .name("No authorization")),
    HTTP_BASIC(ConnDevHttpAuthTypeType.BASIC, new ConnDevAuthInfoType()
            .name("HTTP Basic Authorization")
            .description("Basic authorization using username and password")
            , "Username",
            "Password"),
    HTTP_BEARER(ConnDevHttpAuthTypeType.BEARER, new ConnDevAuthInfoType()
            .name("HTTP Bearer Token Authorization")
            .description("Authorization using Bearer token.")
            , "TokenValue"),
    HTTP_JWT_BEARER(ConnDevHttpAuthTypeType.JWT_BEARER, new ConnDevAuthInfoType()
            .name("HTTP JWT Bearer Token Authorization")
            .description("Authorization using JWT Bearer token.")
            , "JwtTokenName",
            "JwtAlgorithm",
            "JwtSecret",
            "JwtSecretBase64Encoded",
            "JwtPayload",
            "JwtLocation"),
    HTTP_APIKEY(ConnDevHttpAuthTypeType.API_KEY, new ConnDevAuthInfoType()
            .name("HTTP API Key Authorization")
            .description("Authorization using preshared API key"),
            "ApiKey",
            "ApiKeyName",
            "ApiKeyLocation"),
    OAUTH2_CLIENT_CREDENTIALS(ConnDevHttpAuthTypeType.OAUTH2_CLIENT_CREDENTIALS, new ConnDevAuthInfoType()
            .name("OAuth2 Client Credentials")
            .description("Authorization using OAuth2 client credentials flow.")
            , "OAuth2TokenUrl",
            "OAuth2ClientId",
            "OAuth2ClientSecret",
            "OAuth2Scope",
            "OAuth2Audience",
            "OAuth2TokenName",
            "OAuth2ClientAuthenticationScheme"),
    OAUTH2_PASSWORD(ConnDevHttpAuthTypeType.OAUTH2_PASSWORD, new ConnDevAuthInfoType()
            .name("OAuth2 Password Credentials")
            .description("Authorization using OAuth2 password credentials flow.")
            , "OAuth2TokenUrl",
            "OAuth2ClientId",
            "OAuth2ClientSecret",
            "OAuth2Username",
            "OAuth2Password",
            "OAuth2Scope",
            "OAuth2Audience",
            "OAuth2TokenName",
            "OAuth2ClientAuthenticationScheme"),
    OAUTH2_JWT(ConnDevHttpAuthTypeType.OAUTH2_JWT, new ConnDevAuthInfoType()
            .name("OAuth2 JWT Bearer")
            .description("Authorization using OAuth2 JWT bearer flow.")
            , "OAuth2TokenUrl",
            "OAuth2ClientId",
            "OAuth2PrivateKey",
            "OAuth2KeyId",
            "OAuth2Algorithm",
            "OAuth2Issuer",
            "OAuth2Subject",
            "OAuth2Audience",
            "OAuth2ClientAuthenticationScheme"),
    OAUTH2_SAML(ConnDevHttpAuthTypeType.OAUTH2_SAML, new ConnDevAuthInfoType()
            .name("OAuth2 SAML Bearer")
            .description("Authorization using OAuth2 SAML bearer flow.")
            , "OAuth2TokenUrl",
            "OAuth2ClientId",
            "OAuth2PrivateKey",
            "OAuth2Issuer",
            "OAuth2Audience",
            "OAuth2ClientAuthenticationScheme"),
    HTTP_DIGEST(ConnDevHttpAuthTypeType.DIGEST, new ConnDevAuthInfoType()
            .name("HTTP Digest Authorization")
            .description("Digest authorization using username and password")
            , "DigestUsername",
            "DigestPassword",
            "DigestAutoChallenge",
            "DigestMaxRetries",
            "DigestPreemptiveAuth",
            "DigestAlgorithmPreference",
            "DigestStateCacheEnabled"),
    HTTP_HAWK(ConnDevHttpAuthTypeType.HAWK, new ConnDevAuthInfoType()
            .name("Hawk Authorization")
            .description("Authorization using Hawk protocol")
            , "HawkId",
            "HawkKey",
            "HawkAlgorithm",
            "HawkIncludePayloadHash",
            "HawkOffset",
            "HawkExt"),
    AWS_SIGNATURE(ConnDevHttpAuthTypeType.AWS_SIGNATURE, new ConnDevAuthInfoType()
            .name("AWS Signature")
            .description("Authorization using AWS Signature v4")
            , "AwsAccessKey",
            "AwsSecretKey",
            "AwsSessionToken",
            "AwsRegion",
            "AwsService",
            "AwsSignatureVersion"),
    HTTP_NTLM(ConnDevHttpAuthTypeType.NTLM, new ConnDevAuthInfoType()
            .name("NTLM Authorization")
            .description("Authorization using NTLM protocol")
            , "NtlmUsername",
            "NtlmPassword",
            "NtlmDomain",
            "NtlmWorkstation",
            "NtlmVersion"),
    OTHER(ConnDevHttpAuthTypeType.OTHER, new ConnDevAuthInfoType()
            .name("Other")
            .description("Other or unknown authorization type")
            , "OtherCredentials");

    private final List<ItemName.WithoutPrefix> scimProperties;
    private final List<ItemName.WithoutPrefix> restProperties;

    SupportedAuthorization(ConnDevHttpAuthTypeType type, ConnDevAuthInfoType info, String... properties) {
        baseAuthInfo = info
                .type(type);
        scimProperties = Arrays.stream(properties).map(p -> ItemName.from(SchemaConstants.NS_ICF_CONFIGURATION, "scim" + p)).toList();
        restProperties = Arrays.stream(properties).map(p -> ItemName.from(SchemaConstants.NS_ICF_CONFIGURATION, "rest" + p)).toList();
    }

    private final ConnDevAuthInfoType baseAuthInfo;

    public static ConnDevAuthInfoType fromAiType(String type) {
        return switch (type) {
            case "basic" -> HTTP_BASIC.crateBasicInformation();
            case "bearer" -> HTTP_BEARER.crateBasicInformation();
            case "jwtBearer" -> HTTP_JWT_BEARER.crateBasicInformation();
            case "apiKey" -> HTTP_APIKEY.crateBasicInformation();
            case "digest" -> HTTP_DIGEST.crateBasicInformation();
            case "oauth2ClientCredentials" -> OAUTH2_CLIENT_CREDENTIALS.crateBasicInformation();
            case "oauth2Password" -> OAUTH2_PASSWORD.crateBasicInformation();
            case "oauth2Jwt" -> OAUTH2_JWT.crateBasicInformation();
            case "oauth2Saml" -> OAUTH2_SAML.crateBasicInformation();
            case "hawk" -> HTTP_HAWK.crateBasicInformation();
            case "awsSignature" -> AWS_SIGNATURE.crateBasicInformation();
            case "ntlm" -> HTTP_NTLM.crateBasicInformation();
            case "other" -> OTHER.crateBasicInformation();
            default -> null;
        };
    }

    public ConnDevAuthInfoType crateBasicInformation() {
        return baseAuthInfo.clone();
    }

    public static List<ItemName> attributesFor(ConnDevIntegrationType integration, ConnDevHttpAuthTypeType authorizationType) {
        var auth = forAuthorizationType(authorizationType);
        if (auth == null) {
            return List.of();
        }
        return auth.attributesFor(integration);
    }

    public List<ItemName> attributesFor(ConnDevIntegrationType integration) {
        return (List) switch (integration) {
            case SCIM -> scimProperties;
            case REST -> restProperties;
        };
    }

    public static SupportedAuthorization forAuthorizationType(ConnDevHttpAuthTypeType authorizationType) {
        return Arrays.stream(SupportedAuthorization.values()).filter(v -> v.baseAuthInfo.getType() != null && v.baseAuthInfo.getType().equals(authorizationType))
                .findFirst().orElse(null);
    }
}
