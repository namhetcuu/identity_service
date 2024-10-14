package com.devteria.identityservice.service;

import com.devteria.identityservice.dto.request.AuthenticationRequest;
import com.devteria.identityservice.dto.request.IntrospectRequest;
import com.devteria.identityservice.dto.response.AuthenticationResponse;
import com.devteria.identityservice.dto.response.IntrospectResponse;
import com.devteria.identityservice.exception.AppException;
import com.devteria.identityservice.exception.ErrorCode;
import com.devteria.identityservice.repository.UserRepository;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE,makeFinal = true)
public class AuthenticationService {

    UserRepository userRepository;

    @NonFinal
    @Value("${jwt.signerKey}")
    protected String SIGNER_KEY;

    public IntrospectResponse introspect(IntrospectRequest request)
            throws JOSEException, ParseException {
        var token = request.getToken();

        //get verifier above(SIGNDER_KEY...)
        JWSVerifier verifier = new MACVerifier(SIGNER_KEY.getBytes());

        //lay token tu database xuong va parse ra dang signedJWT
        SignedJWT signedJWT = SignedJWT.parse(token);

        //check xem token da het han hay chua
        Date expityTime = signedJWT.getJWTClaimsSet().getExpirationTime();

        //ham nay tra ve true neu verify thanh cong signedJWT dung(noi dung token khong bi thay doi),
        // false neu verify ko thanh cong
        var verified = signedJWT.verify(verifier);

        return IntrospectResponse.builder()
                .valid(verified && expityTime.after(new Date()))
                .build();

    }

    public AuthenticationResponse authenticate(AuthenticationRequest request){

        var user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);

        boolean authenticated = passwordEncoder.matches(request.getPassword(), user.getPassword());
        if(!authenticated)
            throw new AppException(ErrorCode.UNAUTHENTICATED);

        var token = generateToken(user.getUsername());

        return AuthenticationResponse.builder()
                .token(token)
                .authenticated(true)
                .build();

    }
    private String generateToken(String username){
        //dung thuat toan ES512 de ma hoa
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);

        //tao claimset de truoc khi dua cho thk payload below
        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .subject(username)
                .issuer("devteria.com")
                .issueTime(new Date())
                .expirationTime(
                        new Date(Instant.now().plus(1, ChronoUnit.HOURS).toEpochMilli())
                )
                .claim("customClaim","Custom")
                .build();

        //convert claimset above to jsonobject
        Payload payload = new Payload(jwtClaimsSet.toJSONObject());

        //tao object tu header va payload
        JWSObject jwsObject = new JWSObject(header,payload);

        //doan nay la signature(trong do signature bao gom payload va header)
        try{
            jwsObject.sign(new MACSigner(SIGNER_KEY.getBytes()));
            return jwsObject.serialize();
        }catch (JOSEException e){
            log.error("Cannot create token",e);
            throw new RuntimeException(e);
        }

    }
}
