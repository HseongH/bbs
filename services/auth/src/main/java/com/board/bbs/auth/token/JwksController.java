package com.board.bbs.auth.token;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** 업무 서비스가 내부 토큰을 검증할 공개키를 제공한다. 진입점에는 연결하지 않는다. */
@RestController
public class JwksController {

  /** 공개키 목록의 경로. */
  public static final String PATH = "/.well-known/jwks.json";

  private final Map<String, Object> publicKeys;

  JwksController(RSAKey signingKey) {
    this.publicKeys = new JWKSet(signingKey.toPublicJWK()).toJSONObject();
  }

  @GetMapping(PATH)
  Map<String, Object> keys() {
    return publicKeys;
  }
}
