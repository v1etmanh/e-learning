package com.jpd.web.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

public class JwtRoleConverted implements Converter<Jwt, Collection<GrantedAuthority>>{

	private static final String CLIENT_ID = "backended-service";

	@Override
	@SuppressWarnings("unchecked")
	public Collection<GrantedAuthority> convert(Jwt source) {

		Collection<GrantedAuthority> authorities = new ArrayList<>();

		/* ========= 1. realm_access.roles ========= */
		Map<String, Object> realmAccess =
				(Map<String, Object>) source.getClaims().get("realm_access");

		if (realmAccess != null) {
			Object rolesObj = realmAccess.get("roles");
			if (rolesObj instanceof List<?>) {
				authorities.addAll(
						((List<String>) rolesObj).stream()
								.map(role -> "ROLE_" + role.toUpperCase())
								.map(SimpleGrantedAuthority::new)
								.collect(Collectors.toList())
				);
			}
		}

		/* ========= 2. resource_access.backended-service.roles ========= */
		Map<String, Object> resourceAccess =
				(Map<String, Object>) source.getClaims().get("resource_access");

		if (resourceAccess != null) {

			Map<String, Object> client =
					(Map<String, Object>) resourceAccess.get(CLIENT_ID);

			if (client != null) {
				Object rolesObj = client.get("roles");

				if (rolesObj instanceof List<?>) {
					authorities.addAll(
							((List<String>) rolesObj).stream()
									.map(role -> "ROLE_" + role.toUpperCase())
									.map(SimpleGrantedAuthority::new)
									.collect(Collectors.toList())
					);
				}
			}
		}

		return authorities;
	}

}