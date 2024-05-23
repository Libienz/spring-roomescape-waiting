package roomescape.reservation.service;

import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import roomescape.global.exception.IllegalRequestException;
import roomescape.member.service.MemberService;
import roomescape.reservation.domain.Reservation;
import roomescape.reservation.domain.ReservationRepository;
import roomescape.reservation.domain.ReservationWithWaiting;
import roomescape.reservation.domain.Reservations;
import roomescape.reservation.dto.MemberReservation;
import roomescape.reservation.dto.MemberReservationAddRequest;
import roomescape.reservation.dto.ReservationResponse;
import roomescape.reservation.dto.WaitingResponse;
import roomescape.theme.service.ThemeService;
import roomescape.time.service.ReservationTimeService;

@Service
public class ReservationService {

    private final MemberService memberService;
    private final ReservationTimeService reservationTimeService;
    private final ThemeService themeService;
    private final ReservationRepository reservationRepository;

    public ReservationService(MemberService memberService, ReservationTimeService reservationTimeService,
                              ThemeService themeService, ReservationRepository reservationRepository) {
        this.memberService = memberService;
        this.reservationTimeService = reservationTimeService;
        this.themeService = themeService;
        this.reservationRepository = reservationRepository;
    }

    public List<ReservationResponse> findAllReservation() {
        return reservationRepository.findAll().stream()
                .map(ReservationResponse::new)
                .toList();
    }

    public List<ReservationResponse> findAllByMemberAndThemeAndPeriod(Long memberId, Long themeId, LocalDate dateFrom,
                                                                      LocalDate dateTo) {
        return reservationRepository.findByMemberAndThemeAndPeriod(memberId, themeId, dateFrom, dateTo).stream()
                .map(ReservationResponse::new)
                .toList();
    }

    public List<MemberReservation> findMemberReservationWithWaitingStatus(Long memberId) {
        return reservationRepository.findByMemberId(memberId).stream()
                .map(reservation -> new ReservationWithWaiting(reservation, countEarlierReservation(reservation)))
                .map(MemberReservation::new)
                .toList();
    }

    private int countEarlierReservation(Reservation reservation) {
        return reservationRepository.countEarlierReservationOnSlot(
                reservation.getId(),
                reservation.getTimeId(),
                reservation.getThemeId(),
                reservation.getDate()
        );
    }

    public List<WaitingResponse> findReservationsOnWaiting() {
        return reservationRepository.findReservationOnWaiting().stream()
                .map(WaitingResponse::new)
                .toList();
    }

    public ReservationResponse saveMemberReservation(Long memberId, MemberReservationAddRequest request) {
        validateMemberReservationNotExistInSlot(memberId, request);

        Reservation reservation = new Reservation(
                memberService.findById(memberId),
                request.date(),
                reservationTimeService.findById(request.timeId()),
                themeService.findById(request.themeId())
        );

        Reservation saved = reservationRepository.save(reservation);
        return new ReservationResponse(saved);
    }

    private void validateMemberReservationNotExistInSlot(Long memberId, MemberReservationAddRequest request) {
        Reservations sameSlotReservations = new Reservations(reservationRepository.findByDateAndTimeAndTheme(
                request.date(),
                request.timeId(),
                request.themeId()
        ));

        if (sameSlotReservations.hasReservationMadeBy(memberId)) {
            throw new IllegalRequestException("해당 아이디로 진행되고 있는 예약(대기)이 이미 존재합니다");
        }
    }

    public void removeReservation(long id) {
        reservationRepository.deleteById(id);
    }
}
