import { Paper } from "@mui/material";
import React, { useState } from "react";
import { OVERLAY_LOCATION } from "../config";
import { Rnd } from "react-rnd";
import PropTypes from "prop-types";
import { localStorageGet, localStorageSet } from "../utils";

const FULL_WIDTH = 1920;
const FULL_HEIGHT = 1080;

export function Overlay(props) {
    const [state, setState] = useState(localStorageGet("OverlayPreviewPosition") ?? {
        scaleFactor: 0.3,
        offsetX: 0,
        offsetY: 0
    });
    const saveState = (state) => {
        localStorageSet("OverlayPreviewPosition", state);
        setState(state);
    };
    const onResize = (e, direction, ref)  => saveState({ ...state, scaleFactor: ref.offsetWidth / FULL_WIDTH });
    const onDrag = (e, ref) => saveState({ ...state, offsetX: ref.lastX, offsetY: ref.lastY });

    return props.isOverlayPreviewShown && (<Rnd
        position={{ x: state.offsetX, y: state.offsetY }}
        width={FULL_WIDTH * state.scaleFactor}
        height={FULL_HEIGHT * state.scaleFactor}
        onResize={onResize}
        onDrag={onDrag}
        lockAspectRatio={true}
        bounds={"body"}
    >
        <Paper sx={{
            overflow: "hidden",
            width: FULL_WIDTH * state.scaleFactor,
            height: FULL_HEIGHT * state.scaleFactor
        }}>
            <iframe src={OVERLAY_LOCATION} width={FULL_WIDTH} height={FULL_HEIGHT} style={{
                transform: `scale(${state.scaleFactor})`,
                transformOrigin: "top left",
                pointerEvents: "none"
            }}/>
        </Paper>
    </Rnd>);
}
Overlay.propTypes = {
    isOverlayPreviewShown: PropTypes.bool.isRequired,
};
