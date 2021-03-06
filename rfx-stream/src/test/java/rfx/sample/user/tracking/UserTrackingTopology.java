package rfx.sample.user.tracking;

import rfx.core.stream.topology.BaseTopology;
import rfx.core.stream.topology.Pipeline;
import rfx.core.util.LogUtil;
import rfx.core.util.Utils;
import rfx.sample.user.tracking.functors.UserTrackingFunctor;
import rfx.sample.user.tracking.functors.UserTrackingLogTokenizer;


public class UserTrackingTopology extends BaseTopology  {
	
	@Override
	public BaseTopology buildTopology(){
		return Pipeline.create(this)
				.apply(UserTrackingLogTokenizer.class)
				.apply(UserTrackingFunctor.class)		
				.done();
	}	
	
	public static void main(String[] args) {
		int beginPartitionId  = 0;
		int endPartitionId  = 1;
		String kafkaTopic = "user-activity";
		LogUtil.setPrefixFileName(kafkaTopic);
		
		BaseTopology topology = new UserTrackingTopology();
		topology.initKafkaDataSeeders(kafkaTopic, beginPartitionId, endPartitionId).buildTopology().start(800);
		Utils.sleep(2000);
	}
}
