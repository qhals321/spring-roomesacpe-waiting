package nextstep.reservation;

import auth.AuthPrincipal;
import auth.AuthenticationException;
import auth.LoginMember;
import nextstep.reservation.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@RestController
public class ReservationController {

    public final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping("/reservations")
    public ResponseEntity createReservation(@LoginMember AuthPrincipal authPrincipal, @RequestBody ReservationRequest reservationRequest) {
        Long id = reservationService.create(Long.parseLong(authPrincipal.getPrincipal()), reservationRequest);
        return ResponseEntity.created(URI.create("/reservations/" + id)).build();
    }

    @PostMapping("/reservation-waitings")
    public ResponseEntity createReservationWaiting(@LoginMember AuthPrincipal authPrincipal, @RequestBody ReservationWaitingRequest reservationWaitingRequest) {
        ReservationWaitingResponse reservationWaitingResponse = reservationService.createWaiting(Long.parseLong(authPrincipal.getPrincipal()), reservationWaitingRequest);

        if (reservationWaitingResponse.isReserved()) {
            return ResponseEntity.created(URI.create("/reservations/" + reservationWaitingRequest.getScheduleId())).build();
        }
        return ResponseEntity.created(URI.create("/reservation-waitings/" + reservationWaitingRequest.getScheduleId())).build();
    }

    @GetMapping("/reservations")
    public ResponseEntity readReservations(@RequestParam Long themeId, @RequestParam String date) {
        List<Reservation> results = reservationService.findAllByThemeIdAndDate(themeId, date);
        return ResponseEntity.ok().body(results);
    }

    @GetMapping("/reservations/mine")
    public ResponseEntity readMyReservations(@LoginMember AuthPrincipal authPrincipal) {
        if (authPrincipal.isAnonymous()) {
            throw new AuthenticationException();
        }

        List<MyReservationResponse> results = reservationService.findMyReservations(Long.parseLong(authPrincipal.getPrincipal()))
            .stream()
            .map(reservation -> new MyReservationResponse(reservation.getId(), reservation.getSchedule()))
            .collect(Collectors.toList());
        return ResponseEntity.ok().body(results);
    }

    @GetMapping("/reservation-waitings/mine")
    public ResponseEntity readMyWaitings(@LoginMember AuthPrincipal authPrincipal) {
        if (authPrincipal.isAnonymous()) {
            throw new AuthenticationException();
        }

        List<MyWaitingResponse> results = reservationService.findMyWaitings(Long.parseLong(authPrincipal.getPrincipal()))
            .stream()
            .map(waiting -> new MyWaitingResponse(waiting.getId(), waiting.getSchedule(), waiting.getSeq()))
            .collect(Collectors.toList());
        return ResponseEntity.ok().body(results);
    }

    @DeleteMapping("/reservations/{id}")
    public ResponseEntity deleteReservation(@LoginMember AuthPrincipal authPrincipal, @PathVariable Long id) {
        reservationService.deleteById(Long.parseLong(authPrincipal.getPrincipal()), id);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/reservation-waitings/{id}")
    public ResponseEntity deleteReservationWaiting(@LoginMember AuthPrincipal authPrincipal, @PathVariable Long id) {
        reservationService.deleteWaitingById(Long.parseLong(authPrincipal.getPrincipal()), id);

        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity onException(Exception e) {
        return ResponseEntity.badRequest().build();
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity onAuthenticationException(AuthenticationException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
}
