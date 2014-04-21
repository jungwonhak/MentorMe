package com.codepath.wwcmentorme.activities;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.RatingBar;
import android.widget.TextView;

import com.codepath.wwcmentorme.R;
import com.codepath.wwcmentorme.data.DataService;
import com.codepath.wwcmentorme.helpers.Async;
import com.codepath.wwcmentorme.helpers.Utils;
import com.codepath.wwcmentorme.helpers.Constants.UserType;
import com.codepath.wwcmentorme.models.Rating;
import com.codepath.wwcmentorme.models.User;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;

public class ViewProfileActivity extends AppActivity {
	public static final String USER_ID_KEY = "userId";
	public static final String LATITUDE_KEY = "latitude";
	public static final String LONGITUDE_KEY = "longitude";
	private User user;
	private ImageLoader mImageLoader;
	private ImageView ivMentorProfile;
	private Double mLat;
	private Double mLng;
	private TextView tvFirstName;
	private TextView tvLastName;
	private TextView tvPosition;
	private TextView tvLocation;
	private TextView tvDistance;
	private TextView tvMenteeCount;
	private RatingBar rbRating;
	private TextView tvNoOfRating;
	private TextView tvAddReview;
	private TextView tvAboutMe;
	private LinearLayout llMentorSkills;
	private LinearLayout llMenteeSkills;
	private TextView tvYearsExperience;
	private TextView tvMentorSkills;
	private TextView tvMenteeSkills;
	private TextView tvAvailabilityHeader;
	private LinearLayout llAvailability;
	private ImageView ivMentee;
	private MapFragment fragment;
	private Menu menu;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_view_profile);
		
		if(getIntent().hasExtra(LATITUDE_KEY)) {
			mLat = getIntent().getDoubleExtra(LATITUDE_KEY, 0);
		}
		
		if(getIntent().hasExtra(LONGITUDE_KEY)) {
			mLng = getIntent().getDoubleExtra(LONGITUDE_KEY, 0);
		}
		
		if (getIntent().hasExtra(USER_ID_KEY)) {
			final long userId = getIntent().getLongExtra(USER_ID_KEY, 0);
			user = User.getUser(userId);
			setupViews();
			populateViews();
			updateMenuTitles();
		}
	}

	private void setupViews() {
		ivMentorProfile = (ImageView) findViewById(R.id.ivMentorProfile);
		tvFirstName = (TextView) findViewById(R.id.tvFirstName);
		tvLastName = (TextView) findViewById(R.id.tvLastName);
		tvPosition = (TextView) findViewById(R.id.tvPosition);
		tvLocation = (TextView) findViewById(R.id.tvLocation);
		tvDistance = (TextView) findViewById(R.id.tvDistance);
		tvMenteeCount = (TextView) findViewById(R.id.tvMenteeCount);
		rbRating = (RatingBar) findViewById(R.id.rbRating);
		LayerDrawable stars;
		stars = (LayerDrawable) rbRating.getProgressDrawable();
		stars.getDrawable(2).setColorFilter(Color.parseColor("#00B6AA"), PorterDuff.Mode.SRC_ATOP);
		tvNoOfRating = (TextView) findViewById(R.id.tvNoOfRating);
		tvAddReview = (TextView) findViewById(R.id.tvAddReview);
		tvAboutMe = (TextView) findViewById(R.id.tvAboutMe);
		llMentorSkills = (LinearLayout) findViewById(R.id.llMentorSkills);
		llMenteeSkills = (LinearLayout) findViewById(R.id.llMenteeSkills);
		tvYearsExperience = (TextView) findViewById(R.id.tvYearsExperience);
		tvMentorSkills = (TextView) findViewById(R.id.tvMentorSkills);
		tvMenteeSkills = (TextView) findViewById(R.id.tvMenteeSkills);
		tvAvailabilityHeader = (TextView) findViewById(R.id.tvAvailabilityHeader);
		llAvailability = (LinearLayout) findViewById(R.id.llAvailability);
		ivMentee = (ImageView) findViewById(R.id.ivMentee);
		fragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
	}

	private void populateViews() {
		mImageLoader = ImageLoader.getInstance();
		mImageLoader.init(ImageLoaderConfiguration.createDefault(this));
		mImageLoader.displayImage(user.getProfileImageUrl(640), ivMentorProfile);
		
		tvFirstName.setText(user.getFirstName());
		tvLastName.setText(user.getLastName());
		
		String formattedPosition = user.getJobTitle()  + ", " + user.getCompanyName();
		tvPosition.setText(Html.fromHtml(formattedPosition));
		
		String formattedLocation = user.getCity()  + ", " + user.getZip();
		tvLocation.setText(Html.fromHtml(formattedLocation));
		
		Double distance = Utils.getDistance(mLat, mLng, user.getLocation().getLatitude(), user.getLocation().getLongitude());
		tvDistance.setText(Utils.formatDouble(distance) + "mi");	
		tvAboutMe.setText(user.getAboutMe());
		tvYearsExperience.setText(Integer.toString(user.getYearsExperience()));

		populateMenteeCount();
		populateAverageRating();
		populateAddReview();	
		populateMentorSkills();
		populateMenteeSkills();
		populateAvailability();
		populateMap();
		setOnClickMenteeCount();
	}
	
	private void populateMap() {
		final GoogleMap map = fragment.getMap();
		map.setMyLocationEnabled(false);
		fragment.getView().post(new Runnable() {
			@Override
			public void run() {
				LatLngBounds.Builder builder = new LatLngBounds.Builder();

				final ParseGeoPoint pt = user.getLocation();
				final LatLng latlng = new LatLng(pt.getLatitude(), pt
						.getLongitude());
				map.addMarker(new MarkerOptions()
						.icon(BitmapDescriptorFactory
								.defaultMarker(BitmapDescriptorFactory.HUE_CYAN))
						.position(latlng));
				builder.include(latlng);			

				map.moveCamera(CameraUpdateFactory.newLatLngZoom(latlng, 13));
			}
		});

	}

	private void setOnClickMenteeCount() {
		ivMentee.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {
				final Intent intent = new Intent(ViewProfileActivity.this, UserListActivity.class);
				intent.putExtra("usertype", UserType.MENTOR.toString());
				intent.putExtra("userId", user.getFacebookId());
				startActivity(intent);			
			}
		});		
	}

	private void populateMenteeCount() {
		final int count = user.getMentees().size();
		tvMenteeCount.setText(Utils.formatNumber(Integer
				.toString(count))
				+ " "
				+ getResources().getQuantityString(R.plurals.mentee,
						count));
	}

	private void populateAvailability() {
		if(user.getAvailability() != null && user.getAvailability().length() > 0) {
			 for (String day : getResources().getStringArray(R.array.dayofweek_array)) {
				 TextView tvAvailabilityDay = (TextView) getLayoutInflater().inflate(R.layout.availability_textview_item, null);
				 tvAvailabilityDay.setText(day);
				 LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
				params.setMargins(0,0,15,0);
				tvAvailabilityDay.setLayoutParams(params);
				 if(user.getAvailability().toString().contains(day)) {
					 tvAvailabilityDay.setBackgroundColor(getResources().getColor(R.color.actionbar));
					 tvAvailabilityDay.setTextColor(Color.parseColor("#ffffff"));
				 } 
				 llAvailability.addView(tvAvailabilityDay);
			}			
		} else {
			tvAvailabilityHeader.setVisibility(View.GONE);
			llAvailability.setVisibility(View.GONE);
		}
	}

	private void populateMenteeSkills() {
		if(user.getMenteeSkills() != null && user.getMenteeSkills().length() > 0) {
			for(int i = 0; i <= user.getMenteeSkills().length() - 1; i++) {
				TextView tvMenteeSkill = (TextView) getLayoutInflater().inflate(R.layout.skill_textview_item, null);
				try {
					tvMenteeSkill.setText(user.getMenteeSkills().get(i).toString());
				} catch (JSONException e) {
					e.printStackTrace();
				}
				
				LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
				params.setMargins(0,0,10,0);
				tvMenteeSkill.setLayoutParams(params);
				
			    llMenteeSkills.addView(tvMenteeSkill); 
			}
		} else {
			tvMenteeSkills.setVisibility(View.GONE);
		}
	}

	private void populateMentorSkills() {
		if(user.getMentorSkills() != null && user.getMentorSkills().length() > 0) {
			for(int i = 0; i <= user.getMentorSkills().length() - 1; i++) {
				TextView tvMentorSkill = (TextView) getLayoutInflater().inflate(R.layout.skill_textview_item, null);
				try {
					tvMentorSkill.setText(user.getMentorSkills().get(i).toString());
				} catch (JSONException e) {
					e.printStackTrace();
				}				
				LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
				params.setMargins(0,0,10,0);
				tvMentorSkill.setLayoutParams(params);
				
			    llMentorSkills.addView(tvMentorSkill);
			}
		} else {
			tvMentorSkills.setVisibility(View.GONE);
		}
	}

	private void populateAddReview() {
		DataService.getRatingByUser(User.meId(), user.getFacebookId(), new GetCallback<Rating>() {
			@Override
			public void done(Rating rating, ParseException e) {
				if(e == null) {
					if(rating != null) {
						tvAddReview.setText("Edit review");
					} 
				} else {
					tvAddReview.setText("Add review");
					e.printStackTrace();
				}				
			}
		});
	}
	
	private void populateAverageRating() {
		DataService.getAverageRating(user.getFacebookId(), new FindCallback<Rating>() {			
			@Override
			public void done(List<Rating> ratings, ParseException e) {
				if(e == null) {
					if(ratings.size() > 0) {
						float numRating = 0;
						int count = 0;
						for (Rating rating : ratings) {
							if(rating.getRating() > 0) {
								count++;
								numRating += rating.getRating();
							}
						}
						numRating = numRating/count;
						tvNoOfRating.setText(Integer.toString(count));
						rbRating.setRating(numRating);
						
					} else {
						rbRating.setRating(0);
						tvNoOfRating.setText("0");
					}
				} else {
					e.printStackTrace();
				}				
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.view_profile, menu);
		super.onCreateOptionsMenu(menu);
		this.menu = menu;
		updateMenuTitles();
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == R.id.miProfileAction) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	private void updateMenuTitles() {
		if (menu == null || user == null) return;
		final MenuItem item = menu.findItem(R.id.miProfileAction);
		final long meId = User.meId();
		final long currentUserId = user.getFacebookId();
		if (currentUserId == meId) {
			item.setTitle("Edit Profile");
		} else {
			DataService.getMentors(meId, new Runnable() {
				@Override
				public void run() {
					final ArrayList<Long> mentors = User.me().getMentors();
					if (mentors.contains(currentUserId)) {
						item.setTitle("Connected");
					} else {
						item.setTitle("Connect");
					}
				}
			});
		}
	}
}
