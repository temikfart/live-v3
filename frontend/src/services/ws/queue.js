import { addRun, modifyRun, removeRun, setFromSnapshot } from "../../redux/contest/queue";
import { pushLog } from "../../redux/debug";

export const handleMessage = (dispatch, e) => {
    const message = JSON.parse(e.data);
    return;
    // switch (message.type) {
    // case "AddRunToQueue":
    //     dispatch(addRun(message.info));
    //     break;
    // case "RemoveRunFromQueue":
    //     dispatch(removeRun(message.info.id));
    //     break;
    // case "ModifyRunInQueue":
    //     dispatch(modifyRun(message.info));
    //     break;
    // case "QueueSnapshot":
    //     dispatch(setFromSnapshot(message.infos));
    //     break;
    // default:
    //     dispatch(pushLog(`UNKNOWN MESSAGE TYPE: ${message.type}`));
    //     break;
    // }
};
