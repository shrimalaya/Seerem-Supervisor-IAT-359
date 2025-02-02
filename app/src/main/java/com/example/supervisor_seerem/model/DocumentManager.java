package com.example.supervisor_seerem.model;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DocumentManager {
    FirebaseFirestore mRef = FirebaseFirestore.getInstance();
    private List<DocumentSnapshot> workers = new ArrayList<>();
    private List<DocumentSnapshot> sites = new ArrayList<>();
    private List<DocumentSnapshot> supervisors = new ArrayList<>();
    private List<DocumentSnapshot> emergencyInfo = new ArrayList<>();
    private List<DocumentSnapshot> contacts = new ArrayList<>();
    private List<DocumentSnapshot> availabilities = new ArrayList<>();
    private List<DocumentSnapshot> dayLeaves = new ArrayList<>();
    private List<DocumentSnapshot> overtime = new ArrayList<>();
    private List<DocumentSnapshot> userHoursChanges = new ArrayList<>();

    private Supervisor currentUser;

    public Supervisor getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(Supervisor currentUser) {
        this.currentUser = currentUser;
    }

    private DocumentManager() {
        // do nothing (to prevent object instantiation)
    }

    /*
     * Singleton Support
     */
    private static DocumentManager instance;
    public static DocumentManager getInstance() {
        if(instance == null) {
            instance = new DocumentManager();
//            instance.retrieveAllData();
        }
        return instance;
    }

    public List<DocumentSnapshot> getWorkers() {
        return workers;
    }

    public List<DocumentSnapshot> getSites() {
        return sites;
    }

    public List<DocumentSnapshot> getSupervisors() {
        return supervisors;
    }

    public List<DocumentSnapshot> getEmergencyInfo() {
        return emergencyInfo;
    }

    public List<DocumentSnapshot> getContacts() {
        return contacts;
    }

    public List<DocumentSnapshot> getAvailabilities() {
        return availabilities;
    }

    public List<DocumentSnapshot> getDayLeaves() { return dayLeaves; }

    public List<DocumentSnapshot> getOvertime() { return overtime; }

    public List<DocumentSnapshot> getUserHoursChanges() { return userHoursChanges; }

    public void setWorkers(List<DocumentSnapshot> workers) {
        this.workers.clear();
        this.workers.addAll(workers);
    }

    public void setSites(List<DocumentSnapshot> sites) {
        this.sites.clear();
        this.sites.addAll(sites);
    }

    public void setSupervisors(List<DocumentSnapshot> supervisors) {
        this.supervisors.clear();
        this.supervisors.addAll(supervisors);
    }

    public void setEmergencyInfo(List<DocumentSnapshot> emergencyInfo) {
        this.emergencyInfo.clear();
        this.emergencyInfo.addAll(emergencyInfo);
    }

    public void setContacts(List<DocumentSnapshot> contacts) {
        this.contacts.clear();
        this.contacts.addAll(contacts);
    }

    public void setAvailabilities(List<DocumentSnapshot> availabilities) {
        this.availabilities.clear();
        this.availabilities.addAll(availabilities);
    }

    public void setDayLeaves(List<DocumentSnapshot> sickLeaves) {
        this.dayLeaves.clear();
        this.dayLeaves = sickLeaves;
    }

    public void setOvertime(List<DocumentSnapshot> overtime) {
        this.overtime.clear();
        this.overtime = overtime;
    }

    public void setUserHoursChanges(List<DocumentSnapshot> userHoursChanges) {
        this.userHoursChanges.clear();
        this.userHoursChanges = userHoursChanges;
    }

    //TODO: The ASYNC Task may not complete in time for activities to repopulate

    // data callback for async task
    private interface DocListCallback {
        void onCallback(List<DocumentSnapshot> docs);
    }

    public interface RetrieveCallback {
        void onCallback(Boolean result);
    }

    public void retrieveAllData(final RetrieveCallback callback) {
        getSupervisorData(new DocListCallback() {
            @Override
            public void onCallback(List<DocumentSnapshot> docs) {
                setSupervisors(docs);

                getEmergencyData(new DocListCallback() {
                    @Override
                    public void onCallback(List<DocumentSnapshot> docs) {
                        setEmergencyInfo(docs);

                        getContactData(new DocListCallback() {
                            @Override
                            public void onCallback(List<DocumentSnapshot> docs) {
                                setContacts(docs);

                                getWorkersData(new DocListCallback() {
                                    @Override
                                    public void onCallback(List<DocumentSnapshot> docs) {
                                        setWorkers(docs);
                                    }
                                });

                                getAvailabilityData(new DocListCallback() {
                                    @Override
                                    public void onCallback(List<DocumentSnapshot> docs) {
                                        setAvailabilities(docs);
                                    }
                                });

                                getSitesData(new DocListCallback() {
                                    @Override
                                    public void onCallback(List<DocumentSnapshot> docs) {
                                        setSites(docs);
                                        callback.onCallback(true);
                                    }
                                });

                                getDayLeaveData(new DocListCallback() {
                                    @Override
                                    public void onCallback(List<DocumentSnapshot> docs) {
                                        setDayLeaves(docs);
                                        callback.onCallback(true);
                                    }
                                });

                                getOvertimeData(new DocListCallback() {
                                    @Override
                                    public void onCallback(List<DocumentSnapshot> docs) {
                                        setOvertime(docs);
                                        callback.onCallback(true);
                                    }
                                });
                            }
                        });
                    }
                });
            }
        });
    }

    private void getSupervisorData(final DocListCallback callback) {
        mRef.collection(CONSTANTS.SUPERVISORS_COLLECTION)
                .whereEqualTo(CONSTANTS.ID_KEY, currentUser.getId())
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if(task.isComplete()) {
                            System.out.println("TEST3> Size of supervisors = " + task.getResult().getDocuments().size());
                            callback.onCallback(task.getResult().getDocuments());
                        }
                    }
                });
    }

    private void getWorkersData(final DocListCallback callback) {
        mRef.collection(CONSTANTS.WORKERS_COLLECTION)
                .whereEqualTo(CONSTANTS.COMPANY_ID_KEY, currentUser.getCompany_id())
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if(task.isComplete()) {
                            System.out.println("TEST3> Size of workers = " + task.getResult().getDocuments().size());
                            callback.onCallback(task.getResult().getDocuments());
                        }
                    }
                });
    }

    private void getSitesData(final DocListCallback callback) {
        mRef.collection(CONSTANTS.WORKSITES_COLLECTION)
                .whereEqualTo(CONSTANTS.COMPANY_ID_KEY, currentUser.getCompany_id())
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if(task.isComplete()) {
                            System.out.println("TEST3> Size of sites = " + task.getResult().getDocuments().size());
                            callback.onCallback(task.getResult().getDocuments());
                        }
                    }
                });
    }

    private void getAvailabilityData(final DocListCallback callback) {
        mRef.collection(CONSTANTS.AVAILABILITY_COLLECTION)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if(task.isComplete()) {
                            System.out.println("TEST3> Size of availability = " + task.getResult().getDocuments().size());
                            callback.onCallback(task.getResult().getDocuments());
                        }
                    }
                });
    }

    private void getContactData(final DocListCallback callback) {
        mRef.collection(CONSTANTS.CONTACT_INFO_COLLECTION)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if(task.isComplete()) {
                            System.out.println("TEST3> Size of contacts = " + task.getResult().getDocuments().size());
                            callback.onCallback(task.getResult().getDocuments());
                        }
                    }
                });
    }

    private void getEmergencyData(final DocListCallback callback) {
        mRef.collection(CONSTANTS.EMERGENCY_INFO_COLLECTION)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if(task.isComplete()) {
                            System.out.println("TEST3> Size of emergency = " + task.getResult().getDocuments().size());
                            callback.onCallback(task.getResult().getDocuments());
                        }
                    }
                });
    }

    private void getDayLeaveData(final DocListCallback callback) {
        mRef.collection(CONSTANTS.PENDING_USER_HOURS_CHANGES_COLLECTION)
                .document(currentUser.getId())
                .collection(CONSTANTS.PENDING_DAY_LEAVE_COLLECTION)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if(task.isComplete()) {
                            System.out.println("TEST3> Size of sick leave = " + task.getResult().getDocuments().size());
                            callback.onCallback(task.getResult().getDocuments());
                        }
                    }
                });
    }

    // Collection: User Changes > Get the document based on user id
    // -> get the overtime collection associated with that user's id
    private void getOvertimeData(final DocListCallback callback) {
        mRef.collection(CONSTANTS.PENDING_USER_HOURS_CHANGES_COLLECTION)
                .document(currentUser.getId())
                .collection(CONSTANTS.PENDING_OVERTIME_COLLECTION)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if(task.isComplete()) {
                            System.out.println("TEST3> Size of overtime  = " + task.getResult().getDocuments().size());
                            callback.onCallback(task.getResult().getDocuments());
                        }
                    }
                });
    }

//    private void getSitesData(final DocListCallback callback) {
//        mRef.collection(CONSTANTS.WORKSITES_COLLECTION)
//                .document(currentUser.getId())
//                .collection(CONSTANTS.PENDING_DAY_LEAVE_COLLECTION)
//                .get()
//                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
//                    @Override
//                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
//                        if(task.isComplete()) {
//                            System.out.println("TEST3> Size of sites = " + task.getResult().getDocuments().size());
//                            callback.onCallback(task.getResult().getDocuments());
//                        }
//                    }
//                });
//    }
//
//    private void getPendingUserHoursChangesData(final DocListCallback callback) {
//        mRef.collection(CONSTANTS.PENDING_USER_HOURS_CHANGES_COLLECTION)
//                .whereEqualTo(CONSTANTS.ID_KEY, currentUser.getId())
//                .get()
//                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
//                    @Override
//                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
//                        if(task.isComplete()) {
//                            System.out.println("TEST3> Size of overtime  = " + task.getResult().getDocuments().size());
//                            callback.onCallback(task.getResult().getDocuments());
//                        }
//                    }
//                });
//    }

}
