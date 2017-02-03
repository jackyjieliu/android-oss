package com.kickstarter.viewmodels;

import android.support.annotation.NonNull;
import android.util.Pair;

import com.kickstarter.libs.ActivityViewModel;
import com.kickstarter.libs.Environment;
import com.kickstarter.libs.RefTag;
import com.kickstarter.libs.utils.NumberUtils;
import com.kickstarter.libs.utils.ObjectUtils;
import com.kickstarter.models.Project;
import com.kickstarter.services.ApiClientType;
import com.kickstarter.ui.IntentKey;
import com.kickstarter.ui.activities.UpdateActivity;

import okhttp3.Request;
import rx.Observable;
import rx.subjects.BehaviorSubject;
import rx.subjects.PublishSubject;

import static com.kickstarter.libs.rx.transformers.Transformers.neverError;
import static com.kickstarter.libs.rx.transformers.Transformers.takeWhen;

public interface Update {

  interface Inputs {
    /** Call when a project update comments uri request has been made. */
    void goToCommentsRequest(Request request);

    /** Call when a project update uri request has been made. */
    void goToProjectRequest(Request request);
  }

  interface Outputs {
    /** Emits an update to start the comments activity with. */
    Observable<com.kickstarter.models.Update> startCommentsActivity();

    /** Emits a project and a ref tag to start the project activity with. */
    Observable<Pair<Project, RefTag>> startProjectActivity();

    /** Emits a string to display in the toolbar title. */
    Observable<String> updateSequence();

    /** Emits a web view url to display. */
    Observable<String> webViewUrl();
  }

  final class ViewModel extends ActivityViewModel<UpdateActivity> implements Inputs, Outputs {
    private final ApiClientType client;

    public ViewModel(final @NonNull Environment environment) {
      super(environment);

      this.client = environment.apiClient();

      final Observable<com.kickstarter.models.Update> update = intent()
        .map(i -> i.getParcelableExtra(IntentKey.UPDATE))
        .ofType(com.kickstarter.models.Update.class)
        .filter(ObjectUtils::isNotNull);

      update
        .compose(takeWhen(this.goToCommentsRequestSubject))
        .subscribe(this.startCommentsActivity::onNext);

      update
        .map(u -> u.urls().web().update())
        .subscribe(this.webViewUrl::onNext);

      update
        .map(u -> NumberUtils.format(u.sequence()))
        .subscribe(this.updateSequence::onNext);

      update
        .compose(takeWhen(this.goToProjectRequestSubject))
        .switchMap(u -> client
          .fetchProject(String.valueOf(u.projectId()))
          .compose(neverError())
        )
        .subscribe(p -> this.startProjectActivity.onNext(Pair.create(p, RefTag.update())));
    }

    private final PublishSubject<Request> goToCommentsRequestSubject = PublishSubject.create();
    private final PublishSubject<Request> goToProjectRequestSubject = PublishSubject.create();

    private final BehaviorSubject<com.kickstarter.models.Update> startCommentsActivity = BehaviorSubject.create();
    private final BehaviorSubject<Pair<Project, RefTag>> startProjectActivity = BehaviorSubject.create();
    private final BehaviorSubject<String> updateSequence = BehaviorSubject.create();
    private final BehaviorSubject<String> webViewUrl = BehaviorSubject.create();

    public final Inputs inputs = this;
    public final Outputs outputs = this;

    @Override public void goToCommentsRequest(final @NonNull Request request) {
      this.goToCommentsRequestSubject.onNext(request);
    }
    @Override public void goToProjectRequest(final @NonNull Request request) {
      this.goToProjectRequestSubject.onNext(request);
    }

    @Override public @NonNull Observable<com.kickstarter.models.Update> startCommentsActivity() {
      return this.startCommentsActivity;
    }
    @Override public @NonNull Observable<Pair<Project, RefTag>> startProjectActivity() {
      return this.startProjectActivity;
    }
    @Override public @NonNull Observable<String> updateSequence() {
      return this.updateSequence;
    }
    @Override public @NonNull Observable<String> webViewUrl() {
      return this.webViewUrl;
    }
  }
}
