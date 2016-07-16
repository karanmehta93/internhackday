package baybuddies.hackday.com.baybuddies;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import baybuddies.hackday.com.baybuddies.chat.ChatActivity;
import com.squareup.picasso.Picasso;

import java.util.List;

public class MyRecyclerAdapter extends RecyclerView.Adapter<MyRecyclerAdapter.CustomViewHolder> {
  private List<Person> feedItemList;
  private Context mContext;

  public MyRecyclerAdapter(Context context, List<Person> feedItemList) {
    this.feedItemList = feedItemList;
    this.mContext = context;
  }

  @Override
  public CustomViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
    View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_person, null);

    CustomViewHolder viewHolder = new CustomViewHolder(view);
    return viewHolder;
  }

  @Override
  public void onBindViewHolder(CustomViewHolder customViewHolder, int i) {
    Person feedItem = feedItemList.get(i);

    //Download image using picasso library
    Picasso.with(mContext).load(feedItem.imageurl)
           .error(R.drawable.ic_launcher)
           .placeholder(R.drawable.ic_launcher)
           .into(customViewHolder.profileImage);

    //Setting text view title
    customViewHolder.name.setText(feedItem.name);
    customViewHolder.email.setText(feedItem.email);
  }

  View.OnClickListener buttonViewProfileClickListener = new View.OnClickListener() {
    @Override
    public void onClick(View view) {
      CustomViewHolder holder = (CustomViewHolder) view.getTag();
      int position = holder.getPosition();

      Person feedItem = feedItemList.get(position);
      openIntent(feedItem);
    }
  };

  private void openIntent(Person person) {
    try {
      Intent i = new Intent(Intent.ACTION_VIEW);
      i.setData(Uri.parse(person.publicurl));
      mContext.startActivity(i);
    } catch (Exception e) {
      Toast.makeText(mContext, "Profile url is not valid", Toast.LENGTH_SHORT).show();
      e.printStackTrace();
    }
  }

  @Override
  public int getItemCount() {
    return (null != feedItemList ? feedItemList.size() : 0);
  }

  public class CustomViewHolder extends RecyclerView.ViewHolder {
    protected ImageView profileImage;
    protected TextView name;
    protected TextView email;
    protected ImageView buttonViewProfile;
    protected ImageView buttonChat;


    public CustomViewHolder(View view) {
      super(view);
      this.profileImage = (ImageView) view.findViewById(R.id.person_thumbnail_imageview);
      this.buttonViewProfile = (ImageView) view.findViewById(R.id.view_profile);
      this.buttonChat = (ImageView) view.findViewById(R.id.chat_button);
      this.name = (TextView) view.findViewById(R.id.person_name);
      this.email = (TextView) view.findViewById(R.id.person_email);


      this.buttonViewProfile.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
          int itemPosition = getAdapterPosition();
          Person feedItem = feedItemList.get(itemPosition);
          openIntent(feedItem);
        }
      });

      this.buttonChat.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
          Intent intent = new Intent(mContext, ChatActivity.class);
          mContext.startActivity(intent);
        }
      });

    }
  }

}