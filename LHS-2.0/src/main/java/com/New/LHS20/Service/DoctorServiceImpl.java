package com.New.LHS20.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.Formatter;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import com.New.LHS20.Dao.DoctorPrescriptionRepository;
import com.New.LHS20.Dao.DoctorRepository;
import com.New.LHS20.Dao.MonitoringDataRepository;
import com.New.LHS20.Dao.PrescriptionRepository;
import com.New.LHS20.Dao.ReceptionRepository;
import com.New.LHS20.Dao.RegistrationRepository;
import com.New.LHS20.Dao.SlotRepo;
import com.New.LHS20.Dao.SpecialityRepository;
import com.New.LHS20.Dto.RegistrationFormUpdateDto;
import com.New.LHS20.Dto.SpecialityDto;
import com.New.LHS20.Entity.AdmissionForm;
import com.New.LHS20.Entity.Doctor;
import com.New.LHS20.Entity.Doctor_Prescription;
import com.New.LHS20.Entity.MonitoringData;
import com.New.LHS20.Entity.Patient;
import com.New.LHS20.Entity.RegistrationForm;
import com.New.LHS20.Entity.SlotTime;
import com.New.LHS20.Entity.Speciality;
import com.New.LHS20.Exception.ResourceAlreadyExistsException;

@Service
public class DoctorServiceImpl implements DoctorService {

	@Autowired
	private RegistrationRepository registrationRepository;

	@Autowired
	private SpecialityRepository specialityRepository;

	@Autowired
	private DoctorRepository doctorrepository;

	@Autowired
	private MonitoringDataRepository MonitoringDataRepository;

	@Autowired
	private SlotRepo slotRepo;

	@Autowired
	private DoctorPrescriptionRepository doctorPrescriptionRepository;

	@Autowired
	private ReceptionRepository receptionrepo;

	@Autowired
	private PrescriptionRepository prescriptionRepository;

	@Autowired
	private DoctorRepository doctorRepository;

	// Doctor can add his Specialty
	public ResponseEntity addSpec(SpecialityDto specialityDto) {
		RegistrationForm registrationForm = registrationRepository.findByEmail(specialityDto.getEmail());
		if (registrationForm.getRoleName().equals("DOCTOR") || registrationForm.getRoleName().equals("ADMIN")) {
			List<Speciality> specialityList = new ArrayList();
			specialityList.add(new Speciality(specialityDto.getSpeciality(), (int) registrationForm.getUserId()));
			Doctor doctor = doctorrepository.findById(registrationForm.getUserId());
			doctor.setSpeciality(specialityDto.getSpeciality());

			doctorrepository.save(doctor);
			specialityRepository.saveAll(specialityList);
			return new ResponseEntity(specialityList, HttpStatus.ACCEPTED);

		} else {
			return new ResponseEntity("Invalid Authorization", HttpStatus.BAD_REQUEST);
		}
	}

	// Doctor can update his profile(MyProfile)
	public ResponseEntity updateMyProfile(RegistrationFormUpdateDto registrationFormUpdateDto) {

		System.out.println(registrationFormUpdateDto.getUserId());

		List<RegistrationForm> registrationForm = registrationRepository
				.findByUserId(registrationFormUpdateDto.getUserId());

		RegistrationForm registrationForm1 = registrationForm.get(0);

		if (registrationFormUpdateDto == null) {
			throw new RuntimeException("Please Enter All The Fields");

		} else if (registrationForm1.getPhoneNo().equals(registrationFormUpdateDto.getPhoneNo())) {

		} else if (registrationRepository.existsByPhoneNo(registrationFormUpdateDto.getPhoneNo())) {

			throw new ResourceAlreadyExistsException(
					"Phone number already exists please register with different phone number");
		}

		if (registrationRepository.existsById(registrationFormUpdateDto.getUserId())) {

			System.err.println(registrationForm1);

			registrationForm1.setFirstName(registrationFormUpdateDto.getFirstName());
			registrationForm1.setLastName(registrationFormUpdateDto.getLastName());
			registrationForm1.setDob(registrationFormUpdateDto.getDob());
			registrationForm1.setEmail(registrationFormUpdateDto.getEmail());
			registrationForm1.setGender(registrationFormUpdateDto.getGender());
			registrationForm1.setPhoneNo(registrationFormUpdateDto.getPhoneNo());
			registrationForm1.setUsername(registrationFormUpdateDto.getEmail());

			registrationRepository.save(registrationForm1);

			  
		      Doctor doctor1 = doctorRepository.findById(registrationForm1.getUserId());



			  ModelMapper mapper1 = new ModelMapper();
			  mapper1.map(registrationForm1, doctor1);
			  



			  doctorRepository.save(doctor1);
			   
			  return new ResponseEntity("Your Profile Updated Successfully",HttpStatus.OK);

		} else {
			return  new ResponseEntity("Please Enter Valid Username",HttpStatus.BAD_REQUEST);
		}
	}
	
	//Doctor can view his/her profile
	 
			public RegistrationForm viewprofile(long userId) {
				System.err.println(userId);

				Optional<RegistrationForm> registrationForm = registrationRepository.findById(userId);

				if (registrationForm.isPresent()) {

					return registrationForm.get();

				} else {

					throw new RuntimeException("Please provide Correct Id" + userId);
				}

			}


	// Doctor can fetch monitoring data using patientId
	public MonitoringData fetchById(Patient patient) {

		List<MonitoringData> monitoring = MonitoringDataRepository.findByPatient(patient);
		// MonitoringData monitoringdata = monitoring.get(0);
		return monitoring.get(0);

	}

	// doctor adding the slot
	public ResponseEntity addSlot(@RequestBody SlotTime slotTime, Authentication authentication) throws ParseException {

		String username = authentication.getName();
		Date slotDate = new SimpleDateFormat("dd/MM/yyyy").parse(slotTime.getDate());
		CharSequence hours = slotTime.getStartTime();

		DateTimeFormatter formatTimeNow = DateTimeFormatter.ofPattern("HH:mm");
		LocalTime timeNow = LocalTime.parse(hours, formatTimeNow);

		RegistrationForm registrationForm = registrationRepository.findByUsername(username);
		slotTime.setDoctorId(registrationForm.getUserId());
		List<SlotTime> listOfSlots = slotRepo.findByDoctorId(registrationForm.getUserId());
		int booked = 0;
		for (SlotTime slotTimeEntity : listOfSlots) {
			if (registrationForm.getRoleName().equals("DOCTOR")
					&& slotTime.getStartTime().equals(slotTimeEntity.getStartTime())
					&& slotTime.getDate().equals(slotTimeEntity.getDate())
					&& slotTime.getEndTime().equals(slotTimeEntity.getEndTime())) {
				booked++;
			}
		}
		if (booked == 0) {
			slotRepo.save(slotTime);
			List<SlotTime> listOfSlotsOutput = new ArrayList<SlotTime>();
			listOfSlotsOutput.add(slotTime);
			return new ResponseEntity(listOfSlotsOutput, HttpStatus.ACCEPTED);
		} else {
			return new ResponseEntity("Already Added", HttpStatus.BAD_REQUEST);
		}
	}

	// Booking the slot
	public ResponseEntity bookAppointmnet(Authentication authentication, @RequestBody SlotTime bookSlot) {

		String username = authentication.getName();
		RegistrationForm registrationForm = registrationRepository.findByUsername(username);
		bookSlot.setPatientId(registrationForm.getUserId());
		List<SlotTime> listOfSlots = slotRepo.findByDoctorId(bookSlot.getDoctorId());
		Iterator iterator = listOfSlots.iterator();
		int booked = 0;
		while (iterator.hasNext()) {
			SlotTime slotTimeEntity = (SlotTime) iterator.next();
			bookSlot.setId(slotTimeEntity.getId());
			if (slotTimeEntity.getPatientId() == null && slotRepo.existsByDoctorId(bookSlot.getDoctorId())
					&& bookSlot.getStartTime().equals(slotTimeEntity.getStartTime())
					&& bookSlot.getDate().equals(slotTimeEntity.getDate())
					&& bookSlot.getEndTime().equals(slotTimeEntity.getEndTime())) {
				bookSlot.setStatus("Pending");

				slotRepo.save(bookSlot);
				booked++;
			}
		}

		if (booked > 0) {
			return new ResponseEntity("Booked", HttpStatus.ACCEPTED);
		} else {
			return new ResponseEntity("Already Booked", HttpStatus.BAD_REQUEST);
		}
	}

	// confirm slot
	public ResponseEntity confirmSlot(Authentication authentication, @RequestBody SlotTime bookSlot) {

		String username = authentication.getName();
		RegistrationForm registrationForm = registrationRepository.findByUsername(username);
		List<SlotTime> listOfSlots = slotRepo.findByDoctorId(registrationForm.getUserId());
		Iterator iterator = listOfSlots.iterator();
		int booked = 0;
		while (iterator.hasNext()) {
			SlotTime slotTime = (SlotTime) iterator.next();
			if (slotTime.getPatientId() != null && slotRepo.existsByDoctorId(slotTime.getDoctorId())
					&& bookSlot.getStartTime().equals(slotTime.getStartTime())
					&& bookSlot.getDate().equals(slotTime.getDate())) {

				slotTime.setStatus("Confirmed");
				AdmissionForm admissionform = new AdmissionForm();
				admissionform.setDisease(slotTime.getDisease());
				admissionform.setRegdNo(slotTime.getPatientId().intValue());
				admissionform.setAdmissionDate(slotTime.getDate());
				admissionform.setDoctor(registrationForm.getUsername());

				receptionrepo.save(admissionform);

				slotRepo.save(slotTime);

				booked++;
			}
		}

		if (booked > 0) {
			return new ResponseEntity("Confirmed", HttpStatus.ACCEPTED);
		} else {
			return new ResponseEntity("not confirmed", HttpStatus.BAD_REQUEST);
		}

	}

	// cancel slot
	public ResponseEntity cancelslot(Authentication authentication, @RequestBody SlotTime bookSlot) {

		String username = authentication.getName();

		RegistrationForm registrationForm = registrationRepository.findByUsername(username);
		System.err.println(registrationForm);
//	        bookSlot.setPatientId(registrationForm.getUserId());

		List<SlotTime> listOfSlots = slotRepo.findByDoctorId(registrationForm.getUserId());

		int booked = 0;
		for (SlotTime slotTime : listOfSlots) {
			bookSlot.setId(slotTime.getId());
			if (slotTime.getPatientId() != null && slotRepo.existsByDoctorId(registrationForm.getUserId())
					&& bookSlot.getStartTime().equals(slotTime.getStartTime())) {
				bookSlot.setStatus("Cancelled");
				slotRepo.save(bookSlot);
				booked++;
			}
		}

		if (booked > 0) {
			return new ResponseEntity("Cancelled", HttpStatus.ACCEPTED);
		} else {
			return new ResponseEntity("Not Cancelled", HttpStatus.BAD_REQUEST);
		}

	}

	// get appointments by the date
	public ResponseEntity getAppointbydate(Authentication authentication) {
		String uname = authentication.getName();
		RegistrationForm registrationForm = registrationRepository.findByUsername(uname);

		DateTimeFormatter format1 = DateTimeFormatter.ofPattern("dd/MM/yyyy");
		String today = LocalDate.now().format(format1);

		List<SlotTime> slotDate = slotRepo.findByDate(today);
		Iterator iterator = slotDate.iterator();
		List getDate = new ArrayList<>();
		while (iterator.hasNext()) {
			SlotTime slotTime = (SlotTime) iterator.next();
			if (String.valueOf(registrationForm.getUserId()).equals(slotTime.getDoctorId().toString())) {
				slotTime.setDoctorId(registrationForm.getUserId());
				getDate.add(slotTime);
			}
		}
		return new ResponseEntity(getDate, HttpStatus.OK);
	}

	// Doctor can fetch upcomming appointments
	public ResponseEntity getUpcommingAppointments(Authentication authentication) {
		String userName = authentication.getName();
		RegistrationForm form = registrationRepository.findByUsername(userName);
		List<SlotTime> listOfSlots = slotRepo.findByDoctorId(form.getUserId());
		SlotTime slotTime = listOfSlots.get(0);
		Long docId = slotTime.getDoctorId();
		DateTimeFormatter format1 = DateTimeFormatter.ofPattern("dd/MM/yyyy");
		String today = LocalDate.now().format(format1);
		String tillDate = LocalDate.now().plusDays(7).format(format1);

		return new ResponseEntity<>(slotRepo.findBySevenDaysSlots1(today, tillDate, docId), HttpStatus.OK);
	}

	// Doctor can add medicines
	public ResponseEntity addmedicines(Authentication authentication,
			@RequestBody Doctor_Prescription doctorprescription) {
		String userName = authentication.getName();
		Doctor doctor = doctorRepository.findByEmail(userName);
		List<Doctor_Prescription> prescriptionList = prescriptionRepository.findByDoctor(doctor);
		int listCount = 0;
		for (Doctor_Prescription prescription2 : prescriptionList) {

			if (prescription2.getDuration() == null) {
				doctorprescription.setId(prescription2.getId());
				doctorprescription.setDoctor(doctor);
				doctorPrescriptionRepository.save(doctorprescription);
				listCount++;
			}
		}
		if (listCount == 0) {
			doctorprescription.setDoctor(prescriptionList.get(0).getDoctor());
			doctorPrescriptionRepository.save(doctorprescription);
		}
		return new ResponseEntity<>(doctorprescription, HttpStatus.ACCEPTED);
	}

	@Override
	public List<SlotTime> getAvailableSlots() {
		Formatter f = new Formatter();
		LocalDate date = LocalDate.now();
		LocalTime time = LocalTime.now();
		System.err.println(time.getHour() / 12);
		LocalTime time2 = LocalTime.of(time.getHour() / 12, time.getMinute());
		System.err.println(time2);
		DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

		int days = 0;
		List<SlotTime> slotTimeList = new ArrayList<>();
		while (days < 7) {
			List<SlotTime> slotTime = slotRepo.findByDate(date.plusDays(days).format(dateFormatter).toString());
			if (days == 0) {
				if (LocalTime.parse(slotTime.get(0).getStartTime()).isAfter(time2)
						&& slotTime.get(0).getPatientId() == null) {
//					 slotTimeList.add(slotTime.get(0));
				}
			} else if (slotTime.get(0).getPatientId() == null) {
				slotTimeList.addAll(slotTime);
			}
			days++;
		}
		return slotTimeList;
	}

}
