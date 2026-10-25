package com.lunisoft.javastarter.unit.core.ratelimit;

import com.lunisoft.javastarter.core.ratelimit.RateLimitResult;
import com.lunisoft.javastarter.core.ratelimit.RateLimitService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class RateLimitServiceTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @InjectMocks
    private RateLimitService rateLimitService;

    @Nested
    class ResolveIpAddress {

        @Test
        void prefers_the_cloudflare_header_over_the_forwarded_chain_and_remote_addr() {
            var request = new MockHttpServletRequest();
            request.setRemoteAddr("172.18.0.5");
            request.addHeader("X-Forwarded-For", "104.21.70.82");
            request.addHeader("CF-Connecting-IP", "88.180.239.181");

            assertThat(rateLimitService.resolveIpAddress(request)).isEqualTo("88.180.239.181");
        }

        @Test
        void takes_the_first_address_of_the_forwarded_chain_when_cloudflare_is_absent() {
            var request = new MockHttpServletRequest();
            request.setRemoteAddr("172.18.0.5");
            request.addHeader("X-Forwarded-For", "203.0.113.9, 10.0.0.1");

            assertThat(rateLimitService.resolveIpAddress(request)).isEqualTo("203.0.113.9");
        }

        @Test
        void falls_back_to_remote_addr_without_any_proxy_header() {
            var request = new MockHttpServletRequest();
            request.setRemoteAddr("172.18.0.5");

            assertThat(rateLimitService.resolveIpAddress(request)).isEqualTo("172.18.0.5");
        }

        @Test
        void reduces_an_ipv6_client_to_its_64_prefix() {
            var request = new MockHttpServletRequest();
            request.addHeader("CF-Connecting-IP", "2a01:e0a:d3b:ed60:14c0:33c:c1fa:c40c");

            assertThat(rateLimitService.resolveIpAddress(request)).isEqualTo("2a01:e0a:d3b:ed60::/64");
        }
    }

    @Nested
    class NormalizeIpAddress {

        @Test
        void maps_two_rotating_addresses_of_the_same_subscriber_to_one_key() {
            var morning = rateLimitService.normalizeIpAddress("2a01:e0a:d3b:ed60:14c0:33c:c1fa:c40c");
            var evening = rateLimitService.normalizeIpAddress("2a01:e0a:d3b:ed60:9d2e:7b1f:0:1");

            assertThat(morning).isEqualTo(evening).isEqualTo("2a01:e0a:d3b:ed60::/64");
        }

        @Test
        void keeps_distinct_subscribers_apart() {
            var first = rateLimitService.normalizeIpAddress("2a01:e0a:d3b:ed60::1");
            var second = rateLimitService.normalizeIpAddress("2a01:e0a:d3b:ed61::1");

            assertThat(first).isNotEqualTo(second);
        }

        @Test
        void leaves_ipv4_untouched() {
            assertThat(rateLimitService.normalizeIpAddress("88.180.239.181")).isEqualTo("88.180.239.181");
        }

        @Test
        void unwraps_an_ipv4_mapped_ipv6_address() {
            assertThat(rateLimitService.normalizeIpAddress("::ffff:88.180.239.181")).isEqualTo("88.180.239.181");
        }

        @Test
        void ignores_a_zone_id() {
            assertThat(rateLimitService.normalizeIpAddress("fe80::1%eth0")).isEqualTo("fe80:0:0:0::/64");
        }

        @Test
        void returns_an_unparseable_value_verbatim() {
            assertThat(rateLimitService.normalizeIpAddress("not:an:ip")).isEqualTo("not:an:ip");
        }
    }

    @Nested
    class WriteRateLimitResponse {

        @Test
        void writes_the_custom_message_with_the_wait_time_filled_in() throws IOException {
            MockHttpServletResponse response = new MockHttpServletResponse();
            RateLimitResult result = new RateLimitResult(31, 30, 150);

            rateLimitService.writeRateLimitResponse(
                    response, result, "Trop vite. Encore {minutes} min ({seconds} s).");

            assertThat(response.getStatus()).isEqualTo(429);
            assertThat(response.getHeader("Retry-After")).isEqualTo("150");
            assertThat(response.getContentAsString()).contains("Trop vite. Encore 3 min (150 s).");
            assertThat(response.getContentAsString()).contains("RATE_LIMIT_EXCEEDED");
        }

        @Test
        void never_shows_zero_minutes_for_a_short_wait() throws IOException {
            MockHttpServletResponse response = new MockHttpServletResponse();
            RateLimitResult result = new RateLimitResult(6, 5, 20);

            rateLimitService.writeRateLimitResponse(response, result, "Patientez {minutes} minute(s).");

            assertThat(response.getContentAsString()).contains("Patientez 1 minute(s).");
        }

        @Test
        void falls_back_to_the_default_message_when_blank() throws IOException {
            MockHttpServletResponse response = new MockHttpServletResponse();
            RateLimitResult result = new RateLimitResult(101, 100, 60);

            rateLimitService.writeRateLimitResponse(response, result, "  ");

            String expected = RateLimitService.DEFAULT_MESSAGE.replace("{minutes}", "1");
            assertThat(response.getContentAsString()).contains(expected);
        }

        @Test
        void falls_back_to_the_default_message_when_null() throws IOException {
            MockHttpServletResponse response = new MockHttpServletResponse();
            RateLimitResult result = new RateLimitResult(101, 100, 60);

            rateLimitService.writeRateLimitResponse(response, result, null);

            String expected = RateLimitService.DEFAULT_MESSAGE.replace("{minutes}", "1");
            assertThat(response.getContentAsString()).contains(expected);
        }
    }
}
